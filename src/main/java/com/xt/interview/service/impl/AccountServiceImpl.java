package com.xt.interview.service.impl;

import com.xt.interview.dto.AccountDTO;
import com.xt.interview.dto.PageResult;
import com.xt.interview.entity.Account;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.AccountRepository;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.service.AccountService;
import com.xt.interview.util.MD5Util;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class AccountServiceImpl implements AccountService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    private static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public AccountDTO createAccount(AccountDTO dto) {
        if (accountRepository.existsByUsernameAndDeleted(dto.getUsername(), NOT_DELETED)) {
            throw new RuntimeException("用户名已存在");
        }
        Account account = new Account();
        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        String password = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? DEFAULT_PASSWORD : dto.getPassword();
        account.setPassword(MD5Util.encode(password));
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(dto.getIsSuper());
        applyDepartment(account, dto.getDepartmentId());
        account.setDeleted(NOT_DELETED);
        account = accountRepository.save(account);
        return toDtoList(List.of(account)).get(0);
    }

    @Override
    public AccountDTO updateAccount(Long id, AccountDTO dto) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));

        if (!account.getUsername().equals(dto.getUsername()) &&
            accountRepository.existsByUsernameAndDeleted(dto.getUsername(), NOT_DELETED)) {
            throw new RuntimeException("用户名已存在");
        }

        account.setUsername(dto.getUsername());
        account.setName(dto.getName());
        account.setGender(dto.getGender());
        account.setPhone(dto.getPhone());
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            account.setPassword(MD5Util.encode(dto.getPassword()));
        }
        account.setRoleLevel(dto.getRoleLevel());
        account.setStatus(dto.getStatus());
        account.setIsSuper(dto.getIsSuper());
        applyDepartment(account, dto.getDepartmentId());
        account = accountRepository.save(account);
        return toDtoList(List.of(account)).get(0);
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        account.setDeleted(DELETED);
        accountRepository.save(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountById(Long id) {
        Account account = accountRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("账号不存在"));
        return toDtoList(List.of(account)).get(0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountDTO> getAllAccounts() {
        return toDtoList(accountRepository.findByDeleted(NOT_DELETED));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AccountDTO> pageAccounts(String username, Integer status, Integer roleLevel, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 10 : Math.min(size, 100);
        Specification<Account> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("deleted"), NOT_DELETED));
            if (username != null && !username.trim().isEmpty()) {
                predicates.add(builder.like(root.get("username"), "%" + username.trim() + "%"));
            }
            if (status != null) {
                predicates.add(builder.equal(root.get("status"), status));
            }
            if (roleLevel != null) {
                predicates.add(builder.equal(root.get("roleLevel"), roleLevel));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
        Page<Account> result = accountRepository.findAll(
                specification,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "id"))
        );
        return new PageResult<>(
                toDtoList(result.getContent()),
                result.getTotalElements(),
                result.getTotalPages(),
                result.getNumber(),
                result.getSize()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO login(String username, String password) {
        Account account = accountRepository.findByUsernameAndPasswordAndDeleted(
                username, MD5Util.encode(password), NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        if (account.getStatus() != 1) {
            throw new RuntimeException("账号已禁用");
        }
        return toDtoList(List.of(account)).get(0);
    }

    private void applyDepartment(Account account, Long departmentId) {
        if (departmentId == null) {
            account.setDepartmentId(null);
            return;
        }
        if (departmentId.equals(account.getDepartmentId())) {
            return;
        }
        departmentRepository.findByIdAndDeleted(departmentId, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("所属部门不存在"));
        account.setDepartmentId(departmentId);
    }

    private List<AccountDTO> toDtoList(List<Account> accounts) {
        List<Long> departmentIds = accounts.stream()
                .map(Account::getDepartmentId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> departmentNames = new HashMap<>();
        if (!departmentIds.isEmpty()) {
            for (Department department : departmentRepository.findAllById(departmentIds)) {
                departmentNames.put(department.getId(), department.getName());
            }
        }
        return accounts.stream().map(account -> {
            AccountDTO dto = convertToDTO(account);
            if (account.getDepartmentId() != null) {
                dto.setDepartmentName(departmentNames.get(account.getDepartmentId()));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    private AccountDTO convertToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setUsername(account.getUsername());
        dto.setName(account.getName());
        dto.setGender(account.getGender());
        dto.setPhone(account.getPhone());
        dto.setRoleLevel(account.getRoleLevel());
        dto.setStatus(account.getStatus());
        dto.setIsSuper(account.getIsSuper());
        dto.setDepartmentId(account.getDepartmentId());
        dto.setCreateTime(account.getCreateTime());
        dto.setUpdateTime(account.getUpdateTime());
        return dto;
    }
}
