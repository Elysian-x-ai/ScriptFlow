-- =============================================
-- ScriptFlow Database Schema
-- Database: MySQL 8.0+
-- =============================================

CREATE DATABASE IF NOT EXISTS scriptflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE scriptflow;

-- -----------------------------------------
-- System Domain
-- -----------------------------------------

-- Multi-tenant table
CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Tenant ID',
    name VARCHAR(100) NOT NULL COMMENT 'Tenant name',
    code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Tenant code',
    contact_name VARCHAR(50) COMMENT 'Contact person',
    contact_phone VARCHAR(20) COMMENT 'Contact phone',
    expire_time DATETIME COMMENT 'License expiration time',
    status TINYINT DEFAULT 1 COMMENT 'Status: 1=enabled, 0=disabled',
    remark VARCHAR(500) COMMENT 'Remarks',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    INDEX idx_code (code),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Multi-tenant';

-- System user table
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'User ID',
    username VARCHAR(50) NOT NULL COMMENT 'Username',
    password VARCHAR(255) NOT NULL COMMENT 'Password (bcrypt)',
    nickname VARCHAR(50) COMMENT 'Display name',
    email VARCHAR(100) COMMENT 'Email',
    phone VARCHAR(20) COMMENT 'Phone number',
    avatar VARCHAR(500) COMMENT 'Avatar URL',
    status TINYINT DEFAULT 1 COMMENT 'Status: 1=enabled, 0=disabled',
    tenant_id BIGINT COMMENT 'Tenant ID',
    remark VARCHAR(500) COMMENT 'Remarks',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    UNIQUE INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='System user';

-- Role table
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Role ID',
    name VARCHAR(50) NOT NULL COMMENT 'Role name',
    code VARCHAR(50) NOT NULL COMMENT 'Role code',
    description VARCHAR(200) COMMENT 'Role description',
    status TINYINT DEFAULT 1 COMMENT 'Status: 1=enabled, 0=disabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    UNIQUE INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role';

-- User-Role association
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT 'User ID',
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User-Role association';

-- Permission table
CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Permission ID',
    name VARCHAR(50) NOT NULL COMMENT 'Permission name',
    code VARCHAR(100) NOT NULL COMMENT 'Permission code',
    type VARCHAR(20) NOT NULL COMMENT 'Type: menu/button/api',
    parent_id BIGINT DEFAULT 0 COMMENT 'Parent permission ID',
    path VARCHAR(200) COMMENT 'Route path',
    icon VARCHAR(50) COMMENT 'Menu icon',
    sort INT DEFAULT 0 COMMENT 'Sort order',
    status TINYINT DEFAULT 1 COMMENT 'Status: 1=enabled, 0=disabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    INDEX idx_parent (parent_id),
    INDEX idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Permission';

-- Role-Permission association
CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT 'Role ID',
    permission_id BIGINT NOT NULL COMMENT 'Permission ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Role-Permission association';

-- -----------------------------------------
-- Business Domain
-- -----------------------------------------

-- Project table
CREATE TABLE IF NOT EXISTS pro_project (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Project ID',
    name VARCHAR(200) NOT NULL COMMENT 'Project name',
    description TEXT COMMENT 'Project description',
    cover VARCHAR(500) COMMENT 'Cover image URL',
    novel_title VARCHAR(200) COMMENT 'Novel title',
    author VARCHAR(100) COMMENT 'Novel author',
    novel_language VARCHAR(10) DEFAULT 'zh' COMMENT 'Novel language',
    chapter_count INT DEFAULT 0 COMMENT 'Number of chapters',
    status TINYINT DEFAULT 1 COMMENT 'Status: 1=active, 0=archived',
    user_id BIGINT NOT NULL COMMENT 'Owner user ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Project';

-- Novel chapter table
CREATE TABLE IF NOT EXISTS pro_novel_chapter (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Chapter ID',
    project_id BIGINT NOT NULL COMMENT 'Project ID',
    chapter_no INT NOT NULL COMMENT 'Chapter sequence number',
    title VARCHAR(200) COMMENT 'Chapter title',
    content LONGTEXT COMMENT 'Chapter content',
    word_count INT DEFAULT 0 COMMENT 'Word count',
    summary TEXT COMMENT 'Chapter summary',
    content_hash VARCHAR(32) DEFAULT NULL COMMENT 'MD5 hash of content',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    INDEX idx_project (project_id),
    INDEX idx_chapter_no (project_id, chapter_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Novel chapter';

-- Character table
CREATE TABLE IF NOT EXISTS pro_character (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Character ID',
    project_id BIGINT NOT NULL COMMENT 'Project ID',
    name VARCHAR(50) NOT NULL COMMENT 'Character name',
    alias VARCHAR(100) COMMENT 'Character alias/nickname',
    gender VARCHAR(10) COMMENT 'Gender',
    age VARCHAR(20) COMMENT 'Age',
    personality VARCHAR(500) COMMENT 'Personality traits',
    appearance TEXT COMMENT 'Physical appearance',
    background TEXT COMMENT 'Background story',
    description TEXT COMMENT 'Detailed description',
    role_type VARCHAR(50) COMMENT 'Role type: protagonist/antagonist/supporting/etc',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    INDEX idx_project (project_id),
    INDEX idx_name (project_id, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Character';

-- Script table
CREATE TABLE IF NOT EXISTS pro_script (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Script ID',
    project_id BIGINT NOT NULL COMMENT 'Project ID',
    version INT DEFAULT 1 COMMENT 'Current version number',
    yaml_content LONGTEXT COMMENT 'Script YAML content',
    word_count INT DEFAULT 0 COMMENT 'Script word count',
    status TINYINT DEFAULT 0 COMMENT 'Status: 0=draft, 1=generating, 2=completed, 3=failed',
    error_msg TEXT COMMENT 'Error message if failed',
    minio_key VARCHAR(500) COMMENT 'MinIO object key for chapters JSON',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    INDEX idx_project (project_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Script';

-- Script version table
CREATE TABLE IF NOT EXISTS pro_script_version (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Version ID',
    script_id BIGINT NOT NULL COMMENT 'Script ID',
    version_no INT NOT NULL COMMENT 'Version number',
    yaml_content LONGTEXT COMMENT 'Version YAML content',
    diff_content LONGTEXT COMMENT 'Diff from previous version',
    change_log VARCHAR(500) COMMENT 'Change description',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    INDEX idx_script (script_id),
    INDEX idx_version (script_id, version_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Script version';

-- -----------------------------------------
-- Task Domain
-- -----------------------------------------

-- AI task table
CREATE TABLE IF NOT EXISTS pro_task (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Task ID',
    project_id BIGINT NOT NULL COMMENT 'Project ID',
    task_type VARCHAR(50) NOT NULL COMMENT 'Task type: novel_analysis/character_extract/script_generate/script_revise',
    status TINYINT DEFAULT 0 COMMENT 'Status: 0=pending, 1=processing, 2=completed, 3=failed, 4=cancelled',
    progress INT DEFAULT 0 COMMENT 'Progress percentage (0-100)',
    request_params TEXT COMMENT 'Request parameters (JSON)',
    result_data LONGTEXT COMMENT 'Result data (JSON)',
    error_msg TEXT COMMENT 'Error message',
    start_time DATETIME COMMENT 'Start time',
    finish_time DATETIME COMMENT 'Finish time',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    INDEX idx_project (project_id),
    INDEX idx_type (task_type),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI task';

CREATE TABLE IF NOT EXISTS pro_task_log (
                                            id BIGINT NOT NULL PRIMARY KEY COMMENT 'Log ID',
                                            task_id BIGINT NOT NULL COMMENT 'Task ID',
                                            stage VARCHAR(50) COMMENT 'Processing stage',
                                            status TINYINT DEFAULT 0 COMMENT 'Stage status: 0=pending, 1=processing, 2=completed, 3=failed',
                                            message TEXT COMMENT 'Log message',
                                            cost_time BIGINT COMMENT 'Time cost in milliseconds',
                                            create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                            create_by BIGINT DEFAULT 0,
                                            update_by BIGINT DEFAULT 0,
                                            deleted TINYINT DEFAULT 0,
                                            INDEX idx_task (task_id),
                                            INDEX idx_stage (task_id, stage)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Task log';
-- -----------------------------------------
-- Prompt Domain
-- -----------------------------------------

-- Prompt template table
CREATE TABLE IF NOT EXISTS pro_prompt_template (
    id BIGINT NOT NULL PRIMARY KEY COMMENT 'Template ID',
    name VARCHAR(100) NOT NULL COMMENT 'Template name',
    code VARCHAR(50) NOT NULL COMMENT 'Template code (unique)',
    type VARCHAR(20) NOT NULL COMMENT 'Type: system/user',
    category VARCHAR(50) COMMENT 'Category: novel_analysis/character_extract/script_generate/etc',
    content TEXT NOT NULL COMMENT 'Prompt content',
    variables VARCHAR(1000) COMMENT 'Variable definitions (JSON)',
    description VARCHAR(500) COMMENT 'Template description',
    version INT DEFAULT 1 COMMENT 'Version',
    status TINYINT DEFAULT 1 COMMENT 'Status: 1=enabled, 0=disabled',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by BIGINT DEFAULT 0,
    update_by BIGINT DEFAULT 0,
    deleted TINYINT DEFAULT 0,
    UNIQUE INDEX idx_code (code),
    INDEX idx_category (category),
    INDEX idx_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt template';
