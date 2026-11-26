-- ==================== 账号关联部门 ====================
ALTER TABLE `sys_account`
    ADD COLUMN `department_id` BIGINT NULL COMMENT '所属部门ID' AFTER `is_super`,
    ADD INDEX `idx_account_department_id` (`department_id`),
    ADD CONSTRAINT `fk_account_department` FOREIGN KEY (`department_id`) REFERENCES `sys_department` (`id`);
