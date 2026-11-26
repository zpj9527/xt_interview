package com.xt.interview.service.impl;

import com.xt.interview.dto.DepartmentDTO;
import com.xt.interview.entity.Department;
import com.xt.interview.repository.DepartmentRepository;
import com.xt.interview.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class DepartmentServiceImpl implements DepartmentService {

    private static final Integer NOT_DELETED = 0;
    private static final Integer DELETED = 1;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public DepartmentDTO createDepartment(DepartmentDTO dto) {
        String name = requireText(dto.getName(), "部门名称不能为空");
        String code = requireText(dto.getCode(), "部门编码不能为空");
        if (departmentRepository.existsByCode(code)) {
            throw new RuntimeException("部门编码已存在");
        }

        Department department = new Department();
        department.setName(name);
        department.setCode(code);
        department.setDescription(trimToNull(dto.getDescription()));
        department.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        department.setDeleted(NOT_DELETED);
        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    @Override
    public DepartmentDTO updateDepartment(Long id, DepartmentDTO dto) {
        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("部门不存在"));

        String name = requireText(dto.getName(), "部门名称不能为空");
        String code = requireText(dto.getCode(), "部门编码不能为空");
        if (!department.getCode().equals(code) && departmentRepository.existsByCode(code)) {
            throw new RuntimeException("部门编码已存在");
        }

        department.setName(name);
        department.setCode(code);
        department.setDescription(trimToNull(dto.getDescription()));
        if (dto.getStatus() != null) {
            department.setStatus(dto.getStatus());
        }
        department = departmentRepository.save(department);
        return convertToDTO(department);
    }

    @Override
    public void deleteDepartment(Long id) {
        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        department.setDeleted(DELETED);
        departmentRepository.save(department);
    }

    @Override
    @Transactional(readOnly = true)
    public DepartmentDTO getDepartmentById(Long id) {
        Department department = departmentRepository.findByIdAndDeleted(id, NOT_DELETED)
                .orElseThrow(() -> new RuntimeException("部门不存在"));
        return convertToDTO(department);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentDTO> getAllDepartments() {
        return departmentRepository.findByDeleted(NOT_DELETED).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private DepartmentDTO convertToDTO(Department department) {
        DepartmentDTO dto = new DepartmentDTO();
        dto.setId(department.getId());
        dto.setName(department.getName());
        dto.setCode(department.getCode());
        dto.setDescription(department.getDescription());
        dto.setStatus(department.getStatus());
        dto.setCreateTime(department.getCreateTime());
        dto.setUpdateTime(department.getUpdateTime());
        return dto;
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
