-- ============================================================================
-- MCERP 初始化 SQL（SQL 存储模式：MySQL / SQLite）
-- 使用方式：
--   1) 插件已在 SOYS config.yml 启用 storage.backends.mysql / sqlite（SQL 后端装配后
--      表会按实体自动创建，本脚本的 CREATE TABLE IF NOT EXISTS 用于独立建库/建表场景）；
--   2) 执行本脚本完成建表 + 初始数据导入；
--   3) 初始数据 ID 为自然数依次排布，可重复执行不产生重复记录（INSERT IGNORE 兼容 MySQL，SQLite 请用 INSERT OR IGNORE）。
-- 表名/列名与实体注解（@TableName / 字段驼峰→小写下划线）一致，与 YAML 端 data/*.yml 对齐。
-- ============================================================================

-- ---------- 1. 菜单表（内置菜单为下方初始化数据 builtin='Y'，自定义菜单运行时写入 builtin='N'） ----------
CREATE TABLE IF NOT EXISTS erp_menu (
    menu_id    VARCHAR(36)  PRIMARY KEY,
    parent_id  VARCHAR(36)  DEFAULT NULL,
    menu_name  VARCHAR(128) NOT NULL DEFAULT '',
    order_num  INT          NOT NULL DEFAULT 0,
    path       VARCHAR(255) DEFAULT '',
    component  VARCHAR(255) DEFAULT NULL,
    menu_type  CHAR(1)      DEFAULT 'C',
    perms      VARCHAR(128) DEFAULT NULL,
    icon       VARCHAR(64)  DEFAULT NULL,
    visible    CHAR(1)      DEFAULT '0',
    status     CHAR(1)      DEFAULT '0',
    builtin    CHAR(1)      DEFAULT 'N'
);

-- ---------- 2. 参数配置表 ----------
CREATE TABLE IF NOT EXISTS erp_config (
    config_id    VARCHAR(36)  PRIMARY KEY,
    config_name  VARCHAR(128) NOT NULL DEFAULT '',
    config_key   VARCHAR(128) NOT NULL DEFAULT '',
    config_value VARCHAR(255) DEFAULT '',
    config_type  CHAR(1)      DEFAULT 'N',
    remark       VARCHAR(255) DEFAULT NULL,
    create_time  VARCHAR(32)  DEFAULT NULL
);

-- ---------- 3. 字典类型表 ----------
CREATE TABLE IF NOT EXISTS erp_dict_type (
    dict_id     VARCHAR(36)  PRIMARY KEY,
    dict_name   VARCHAR(128) NOT NULL DEFAULT '',
    dict_type   VARCHAR(64)  NOT NULL DEFAULT '',
    status      CHAR(1)      DEFAULT '0',
    remark      VARCHAR(255) DEFAULT NULL,
    create_time VARCHAR(32)  DEFAULT NULL
);

-- ---------- 4. 字典数据表 ----------
CREATE TABLE IF NOT EXISTS erp_dict_data (
    dict_code  VARCHAR(36)  PRIMARY KEY,
    dict_sort  INT          NOT NULL DEFAULT 0,
    dict_label VARCHAR(128) NOT NULL DEFAULT '',
    dict_value VARCHAR(128) NOT NULL DEFAULT '',
    dict_type  VARCHAR(64)  NOT NULL DEFAULT '',
    status     CHAR(1)      DEFAULT '0',
    remark     VARCHAR(255) DEFAULT NULL
);

-- ---------- 5. 通知公告表 ----------
CREATE TABLE IF NOT EXISTS erp_notice (
    notice_id      VARCHAR(36)  PRIMARY KEY,
    notice_title   VARCHAR(128) NOT NULL DEFAULT '',
    notice_type    CHAR(1)      DEFAULT '1',
    notice_content TEXT,
    status         CHAR(1)      DEFAULT '0',
    create_by      VARCHAR(64)  DEFAULT '',
    create_time    VARCHAR(32)  DEFAULT NULL
);

-- ---------- 6. 用户表 ----------
CREATE TABLE IF NOT EXISTS erp_user (
    user_id      VARCHAR(36)  PRIMARY KEY,
    user_name    VARCHAR(64)  NOT NULL DEFAULT '',
    nick_name    VARCHAR(64)  NOT NULL DEFAULT '',
    email        VARCHAR(128) DEFAULT '',
    phonenumber  VARCHAR(32)  DEFAULT '',
    sex          CHAR(1)      DEFAULT '0',
    status       CHAR(1)      DEFAULT '0',
    remark       VARCHAR(255) DEFAULT NULL,
    create_time  VARCHAR(32)  DEFAULT NULL
);

-- ---------- 7. 操作日志表 ----------
CREATE TABLE IF NOT EXISTS erp_oper_log (
    oper_id        VARCHAR(36)  PRIMARY KEY,
    title          VARCHAR(128) DEFAULT '',
    business_type  INT          DEFAULT 0,
    method         VARCHAR(255) DEFAULT '',
    request_method VARCHAR(16)  DEFAULT '',
    oper_name      VARCHAR(64)  DEFAULT '',
    oper_url       VARCHAR(255) DEFAULT '',
    oper_ip        VARCHAR(64)  DEFAULT '',
    oper_param     TEXT,
    json_result    TEXT,
    status         INT          DEFAULT 0,
    error_msg      TEXT,
    oper_time      VARCHAR(32)  DEFAULT NULL
);

-- ---------- 8. 登录日志表 ----------
CREATE TABLE IF NOT EXISTS erp_logininfor (
    info_id    VARCHAR(36) PRIMARY KEY,
    user_name  VARCHAR(64)  DEFAULT '',
    ipaddr     VARCHAR(64)  DEFAULT '',
    status     CHAR(1)      DEFAULT '0',
    msg        VARCHAR(255) DEFAULT '',
    login_time VARCHAR(32)  DEFAULT NULL
);

-- ============================================================================
-- 初始数据（与 resources/data/*.yml 保持一致）
-- ============================================================================

-- 参数配置（2 条）
INSERT IGNORE INTO erp_config (config_id, config_name, config_key, config_value, config_type, remark, create_time) VALUES
('1', '验证码开关',   'sys.account.captchaEnabled', 'false', 'Y', '系统内置参数', '2026-09-11 00:00:00'),
('2', '注册用户开关', 'sys.account.registerUser',   'false', 'Y', '系统内置参数', '2026-09-11 00:00:00');

-- 字典类型（3 条）
INSERT IGNORE INTO erp_dict_type (dict_id, dict_name, dict_type, status, remark, create_time) VALUES
('1', '系统开关', 'system_normal_disable', '0', '系统内置字典', '2026-09-11 00:00:00'),
('2', '系统是否', 'sys_yes_no',           '0', '系统内置字典', '2026-09-11 00:00:00'),
('3', '用户性别', 'sys_user_sex',         '0', '系统内置字典', '2026-09-11 00:00:00');

-- 字典数据（7 条）
INSERT IGNORE INTO erp_dict_data (dict_code, dict_sort, dict_label, dict_value, dict_type, status) VALUES
('1', 0, '正常', '0', 'system_normal_disable', '0'),
('2', 1, '停用', '1', 'system_normal_disable', '0'),
('3', 0, '是',   'Y', 'sys_yes_no',           '0'),
('4', 1, '否',   'N', 'sys_yes_no',           '0'),
('5', 0, '男',   '0', 'sys_user_sex',         '0'),
('6', 1, '女',   '1', 'sys_user_sex',         '0'),
('7', 2, '未知', '2', 'sys_user_sex',         '0');

-- 菜单（40 条：9 菜单节点 + 31 按钮权限；ID 为自然数，builtin='Y' 标记内置，控制器据此禁止修改/删除）
INSERT IGNORE INTO erp_menu (menu_id, parent_id, menu_name, order_num, path, component, menu_type, perms, icon, visible, status, builtin) VALUES
('1', '0', '系统管理', 1, '/system', 'Layout',                  'M', 'system:dir:view',                    'system',    '0', '0', 'Y'),
('2', '1', '用户管理', 1, 'user',      'system/user/index',         'C', 'system:user:list',           'user',      '0', '0', 'Y'),
('3', '1', '菜单管理', 2, 'menu',      'system/menu/index',         'C', 'system:menu:list',           'menu',      '0', '0', 'Y'),
('4', '1', '字典管理', 3, 'dict',      'system/dict/index',         'C', 'system:dict:list',           'dict',      '0', '0', 'Y'),
('5', '1', '参数设置', 4, 'config',    'system/config/index',       'C', 'system:config:list',         'config',    '0', '0', 'Y'),
('6', '1', '通知公告', 5, 'notice',    'system/notice/index',       'C', 'system:notice:list',         'notice',    '0', '0', 'Y'),
('7', '1', '日志管理', 6, 'log',       'Layout',                    'M', 'monitor:dir:view',                    'log',       '0', '0', 'Y'),
('8', '7', '操作日志', 1, 'operlog',   'monitor/operlog/index',     'C', 'monitor:operlog:list',       'operlog',   '0', '0', 'Y'),
('9', '7', '登录日志', 2, 'logininfor','monitor/logininfor/index',  'C', 'monitor:logininfor:list',    'logininfor','0', '0', 'Y'),
('10', '2', '用户查询', 1, '', '', 'F', 'system:user:query',      '', '0', '0', 'Y'),
('11', '2', '用户新增', 2, '', '', 'F', 'system:user:add',        '', '0', '0', 'Y'),
('12', '2', '用户修改', 3, '', '', 'F', 'system:user:edit',       '', '0', '0', 'Y'),
('13', '2', '用户删除', 4, '', '', 'F', 'system:user:remove',     '', '0', '0', 'Y'),
('14', '2', '重置密码', 5, '', '', 'F', 'system:user:resetPwd',   '', '0', '0', 'Y'),
('15', '2', '用户状态', 6, '', '', 'F', 'system:user:changeStatus','', '0', '0', 'Y'),
('16', '2', '用户导出', 7, '', '', 'F', 'system:user:export',     '', '0', '0', 'Y'),
('17', '3', '菜单查询', 1, '', '', 'F', 'system:menu:query',      '', '0', '0', 'Y'),
('18', '3', '菜单新增', 2, '', '', 'F', 'system:menu:add',        '', '0', '0', 'Y'),
('19', '3', '菜单修改', 3, '', '', 'F', 'system:menu:edit',       '', '0', '0', 'Y'),
('20', '3', '菜单删除', 4, '', '', 'F', 'system:menu:remove',     '', '0', '0', 'Y'),
('21', '4', '字典查询', 1, '', '', 'F', 'system:dict:query',      '', '0', '0', 'Y'),
('22', '4', '字典新增', 2, '', '', 'F', 'system:dict:add',        '', '0', '0', 'Y'),
('23', '4', '字典修改', 3, '', '', 'F', 'system:dict:edit',       '', '0', '0', 'Y'),
('24', '4', '字典删除', 4, '', '', 'F', 'system:dict:remove',     '', '0', '0', 'Y'),
('25', '5', '参数查询', 1, '', '', 'F', 'system:config:query',    '', '0', '0', 'Y'),
('26', '5', '参数新增', 2, '', '', 'F', 'system:config:add',      '', '0', '0', 'Y'),
('27', '5', '参数修改', 3, '', '', 'F', 'system:config:edit',     '', '0', '0', 'Y'),
('28', '5', '参数删除', 4, '', '', 'F', 'system:config:remove',   '', '0', '0', 'Y'),
('29', '6', '公告查询', 1, '', '', 'F', 'system:notice:query',    '', '0', '0', 'Y'),
('30', '6', '公告新增', 2, '', '', 'F', 'system:notice:add',      '', '0', '0', 'Y'),
('31', '6', '公告修改', 3, '', '', 'F', 'system:notice:edit',     '', '0', '0', 'Y'),
('32', '6', '公告删除', 4, '', '', 'F', 'system:notice:remove',   '', '0', '0', 'Y'),
('33', '8', '日志查询', 1, '', '', 'F', 'monitor:operlog:query',  '', '0', '0', 'Y'),
('34', '8', '日志导出', 2, '', '', 'F', 'monitor:operlog:export', '', '0', '0', 'Y'),
('35', '8', '日志删除', 3, '', '', 'F', 'monitor:operlog:remove', '', '0', '0', 'Y'),
('36', '8', '日志清空', 4, '', '', 'F', 'monitor:operlog:clean',  '', '0', '0', 'Y'),
('37', '9', '日志查询', 1, '', '', 'F', 'monitor:logininfor:query',  '', '0', '0', 'Y'),
('38', '9', '日志导出', 2, '', '', 'F', 'monitor:logininfor:export', '', '0', '0', 'Y'),
('39', '9', '日志删除', 3, '', '', 'F', 'monitor:logininfor:remove', '', '0', '0', 'Y'),
('40', '9', '日志清空', 4, '', '', 'F', 'monitor:logininfor:clean',  '', '0', '0', 'Y');

-- 旧版初始化数据清理（曾用 00000000-0000-4000-8000-000000000xxx 格式 UUID 主键；
-- 升级到自然数 ID 后需先清理旧记录再导入，否则内置数据重复两份）：
-- DELETE FROM erp_config  WHERE config_id  LIKE '00000000-%';
-- DELETE FROM erp_dict_type WHERE dict_id    LIKE '00000000-%';
-- DELETE FROM erp_dict_data WHERE dict_code  LIKE '00000000-%';
-- DELETE FROM erp_menu      WHERE menu_id    LIKE '00000000-%';
