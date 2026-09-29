# 05 · Data & Initialization

MCERP uses an **annotation-driven storage layer** (the main plugin's dlz entity engine).

The same entities support both **YAML file mode** and **SQL mode** (MySQL / SQLite).

All 8 business tables use `BIGINT AUTO_INCREMENT` primary keys.

## 1. Storage modes



| Mode | Seed source                                                              | Runtime data location                     |
| ---- | ------------------------------------------------------------------------ | ----------------------------------------- |
| YAML | `src/main/resources/data/*.yml` → released into the main plugin data dir | `plugins/SOYSHTTPOverMC/data/<table>.yml` |
| SQL  | `src/main/resources/sql/mcerp-init.sql`                                  | DB tables (auto-created from entities)    |



* `storage.backends.mysql / sqlite` in the main plugin `config.yml` decides the backend;

* In SQL mode tables are auto-created from entity annotations; `mcerp-init.sql` covers standalone

  DB setup (`CREATE TABLE IF NOT EXISTS`, idempotent); seed rows use `INSERT IGNORE`

  (`INSERT OR IGNORE` for SQLite).

## 2. Tables (8)



| Table            | Entity          | Purpose                                                          |
| ---------------- | --------------- | ---------------------------------------------------------------- |
| `erp_menu`       | `ErpMenu`       | Menus (builtin seed `builtin='Y'`, runtime custom `builtin='N'`) |
| `erp_config`     | `ErpConfig`     | Parameters                                                       |
| `erp_dict_type`  | `ErpDictType`   | Dict types                                                       |
| `erp_dict_data`  | `ErpDictData`   | Dict data                                                        |
| `erp_notice`     | `ErpNotice`     | Notices                                                          |
| `erp_user`       | `ErpUser`       | Users                                                            |
| `erp_oper_log`   | `ErpOperLog`    | Oper logs                                                        |
| `erp_logininfor` | `ErpLogininfor` | Login logs                                                       |

Primary keys: `erp_menu.menu_id`, `erp_config.config_id`, `erp_dict_type.dict_id`, `erp_dict_data.dict_code`,

`erp_notice.notice_id`, `erp_user.user_id`, `erp_oper_log.oper_id`, `erp_logininfor.info_id`

— all `BIGINT AUTO_INCREMENT` (`ErpMenu.parentId` numeric too).

## 3. Time field conventions



* Stored as `yyyy-MM-dd HH:mm:ss` strings (no millisecond timestamps);

* Entity fields: `Date` + `@JsonFormat(pattern = BeanCodec.DATE_TIME_PATTERN)`:



```
/\*\* Update time \*/

@JsonFormat(pattern = BeanCodec.DATE\_TIME\_PATTERN)

private Date updateTime;
```



* Business entities extend `BaseEntity` (createTime/updateTime/createBy/updateBy/remark),

  filled by the storage layer on write.

## 4. Seed data

### YAML mode

`data/*.yml` align with entities by table name (camelCase → lowercase\_underscore),

e.g. `erp_menu.yml` ships the builtin menus (\~40 entries: Dashboard, System, User, Menu, Dict,

Config, Notice, Logs, ...).

### SQL mode

`mcerp-init.sql` contains:



* `CREATE TABLE IF NOT EXISTS` for all 8 tables;

* seed rows aligned with the YAML side (natural-number IDs, safe to re-run).

## 5. Addon data seeding (SoysExpansion capability)

Addon ERP plugins (`McerpExpansion`) can provide their own seed data via:



| Hook              | Notes                                                                                |
| ----------------- | ------------------------------------------------------------------------------------ |
| `dataRoots()`     | resource dir names (e.g. `{"data"}`); `*.yml` released into the main plugin data dir |
| `sqlRoots()`      | SQL resource dir names                                                               |
| `seedData()`      | `List<Object>` seed entities (inserted at runtime)                                   |
| `schemaVersion()` | data version for upgrades                                                            |

Executed by the main plugin in a built-in transaction with meta-driven idempotency.