# 03 · Core Features

This page describes the built-in admin modules and backend implementation conventions.

## 1. Feature list

| Menu | Module | Notes |
| --- | --- | --- |
| Dashboard | `/` | Stats panel: main/addon/total module counts, dir/menu/perm counts, module list |
| System | User management | CRUD, status toggle, permission assignment (menu tree + button perms), reset password, profile |
| System | Menu management | Menu tree (M dir / C menu / F button), assign permissions |
| System | Dict management | Dict types + dict data |
| System | Config (parameters) | e.g. `sys.user.initPassword` default password |
| System | Notice | Notice CRUD |
| System | Log management | Oper log + login log (success/failure) |

> Addon ERP modules (`McerpExpansion`) merge their menus into the sidebar and route table live —
> see [04-Extension-Development](04-Extension-Development.md).

## 2. Controller & layering conventions (mandatory)

- **Zero logic in controllers**: controllers only receive params and delegate; all logic lives in
  Service (abstract) → Impl (implementation with transactions);
- **No Map-built responses**: always use entity/VO classes; no temporary `Map<String,String>` assembly;
- **No `@RequestBody String`**: always bind to an entity (`ErpUser`, `ChangeStatusVO`, ...) — prevents manual body parsing;
- **Entity name = table name**: `ErpUser` ↔ `erp_user`, `ErpMenu` ↔ `erp_menu`;
- **VO fields use table field names**: `menuId/menuName/menuType/parentId/orderNum/path/component/perms/icon/visible/status`;
- **Time fields**: unified `yyyy-MM-dd HH:mm:ss`, `Date` + `@JsonFormat(pattern = BeanCodec.DATE_TIME_PATTERN)`,
  business entities extend `BaseEntity` (createTime/updateTime/createBy/updateBy/remark);
- **Permission ids carry plugin prefix**: e.g. `mcerp:system:user:add`;
- **i18n**: all messages use `McerpI18n.t("mcerp.xxx", "Chinese fallback", args...)`.

## 3. Users & permissions

- `erp_user`: `userId` is `BIGINT AUTO_INCREMENT`; `userName` maps to the SOYS credential subject (player name).
- **First-visit auto-registration**: any player logging into ERP for the first time is auto-registered into
  `erp_user` (via `currentPlayer()`), ready for button-permission assignment.
- Permission assignment (User → Assign perms): menu-tree based (like the RuoYi frontend); dirs/menus/buttons
  are checked and saved per user. The permission CURD itself is backed by the main plugin SOYSHTTPOverMC's
  local permission storage; MCERP only renders and persists.
- **OP has all permissions** by default (permission mirror).

## 4. Menu management

Menu table fields (entity `ErpMenu`, VO `ErpMenuVO`):

| Field | Notes |
| --- | --- |
| `menuId` | PK (BIGINT AUTO); `ErpMenuVO` uses String carrying table PK / declared plugin id / composite key |
| `parentId` | Parent id (top-level `0`) |
| `menuName` | Menu name |
| `orderNum` | Sort order (ascending) |
| `path` | Route path (relative to parent) |
| `component` | Component path / page URL (C menu: component or iframe target) |
| `query` / `routeName` | Route query / route name |
| `isFrame` / `isCache` | External link / cache flag |
| `menuType` | `M` dir / `C` menu / `F` button |
| `perms` | Permission id |
| `icon` / `visible` / `status` | Icon / visibility / status |
| `builtin` | `Y` builtin seed / `N` runtime custom |

## 5. Dict / Config / Notice

- Dicts: `erp_dict_type` + `erp_dict_data`; all dict dropdowns read these tables;
- Config: `erp_config`, e.g. `sys.user.initPassword`;
- Notice: `erp_notice`, list/detail.

## 6. Dashboard stats panel

`ErpDashboardServiceImpl` computes live:

- main module count (MCERP itself);
- addon module count (registered `McerpExpansion` modules);
- total modules;
- dirs (M) / menus (C) / perms (F) counts;
- module list (id + display name + menu count).

> The RuoYi welcome/home page was fully removed; the dashboard is the home page and the footer is hidden.

## 7. Removed RuoYi content

- Organization (dept) / post modules: fully removed frontend + backend;
- Code generator (`tool/gen`) and scheduled jobs (`monitor/job`): removed;
- File header comments `Copyright (c) 2019 ruoyi`, RuoYi/Git, RuoYi/Doc components: replaced/removed;
- "Source code / Docs" buttons and "Lock screen" button: removed/commented out.
