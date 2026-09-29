# 04 · Extension Development (Addon ERP Modules)

The core extension path: **an addon plugin extends `McerpExpansion` (which extends the main plugin's `SoysExpansion`)**,
overrides the ERP declaration hooks and calls `register()` once. API registration, static hosting,
data initialization and menu/route registration all happen automatically — install-and-go, uninstall-and-gone.

## 1. Minimal example

```java
package com.example.stock;

import com.github.cocosoys.mc.mcerp.ErpMenus;
import com.github.cocosoys.mc.mcerp.McerpExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class StockPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // One-line registration: SoysExpansion skeleton registers endpoints + hosting + data;
        // onRegister() then registers the module (identifier/displayName/menus) into ErpRegistry.
        if (!new StockExpansion().register()) {
            getLogger().warning("stock register failed (identifier conflict / bootstrap not ready)");
        }
    }
}

public class StockExpansion extends McerpExpansion {

    @Override
    public String getIdentifier() {
        return "stock"; // required: decides /api/plugins/stock/* and /web/plugins/stock/* prefixes
    }

    @Override
    protected String displayName() {
        return "Stock";
    }

    @Override
    protected int sortOrder() {
        return 20;
    }

    @Override
    protected ErpMenus menus() {
        return ErpMenus.create()
            .dir("system", "System", "system", d -> d
                .menu("user", "Users", "user").perm("soys.erp.user.list")
                .menu("group", "Groups", "lock"))
            .menu("home", "Home", "home").component("erp/home");
    }

    @Override
    protected String[] dataRoots() {
        return new String[]{"data"}; // optional: seed data from resources/data/*.yml
    }
}
```

## 2. Overridable hooks (all optional except getIdentifier)

| Hook | Default | Purpose |
| --- | --- | --- |
| `getIdentifier()` | — | **Required**: module id, decides API/page prefixes |
| `displayName()` | identifier | Module name in sidebar |
| `icon()` | null | Module icon |
| `sortOrder()` | 0 | Module sort order |
| `permission()` | null | Access permission for the module menu |
| `homeUrl()` | null | Default page when no children |
| `menus()` | null | Declarative menu tree (`ErpMenus` DSL; preferred over `menusTable()`) |
| `menusTable()` | null | Direct `List<ErpMenuVO>` tree (build nodes yourself) |
| `wujie()` | true | true = wujie micro-frontend / false = classic iframe |
| `dataRoots()` / `sqlRoots()` / `seedData()` / `schemaVersion()` | inherited from SoysExpansion | Data seeding (see 05-Data-and-Initialization) |
| `resourceRoot()` / `indexFallbackEnabled()` / `spaFallback()` | inherited from SoysExpansion | Static hosting |

## 3. Menu DSL (`ErpMenus`)

Method names decide node type: `dir`=M dir, `menu`=C menu, `perm`=F button, `route`=path→component C menu.

### 3.1 Lambda nesting (recommended)

```java
return ErpMenus.create()
    .dir("system", "System", "system", d -> d        // dir; lambda body is child level
        .menu("user", "Users", "user")               // leaf menu
            .perm("soys.erp.user.list")              // button attached to current level
            .perm("soys.erp.user.add")
        .menu("group", "Groups", "lock"))
    .menu("home", "Home", "home").component("erp/home"); // top-level leaf
```

### 3.2 Fluent + up()

```java
return ErpMenus.create()
    .dir("system", "System", "system")   // enter child context
        .menu("user", "Users", "user")
        .menu("group", "Groups", "lock")
    .up()                                 // back to parent level
    .menu("home", "Home", "home").component("erp/home");
```

### 3.3 Full-attribute spec

```java
return ErpMenus.create()
    .dir(s -> s.menuId("system").menuName("System").icon("system").orderNum(1), d -> d
        .menu(m -> m.menuId("user").menuName("Users").icon("user")
            .perms("soys.erp.user.list").orderNum(10).component("erp/user").visible(true))
        .perm(p -> p.menuId("user:add").menuName("Add user").perms("soys.erp.user.add")))
    .route("stock/io", "erp/io").perms("stock:io:list");
```

`MenuSpec` supports all table fields: `menuId/menuName/parentId/orderNum/path/component/componentFull/
componentFullURL/query/routeName/isFrame/isCache/menuType/visible/status/perms/icon/builtin`.

### 3.4 component auto-prefixing

- `component("erp/user")` → auto-prefixed with `pageDirRoot()` = `/web/plugins/<id>/<safeDir>/erp/user`;
- `componentFull("xxx")` → prefixed only with `/web/plugins/<id>`;
- `componentFullURL("https://example.com/page")` → used verbatim (external link);
- **No component at all**: C menus get `basePrefix + parent dir chain + "/" + menuId`
  (e.g. `dir("config") > dir("dict") > menu("type")` → `/web/plugins/<id>/<safe>/config/dict/type`).

## 4. Auto buttons (Controller → F buttons)

Pass a controller class to `menu()` or chain `permsFrom(Class...)`; `ControllerPermScanner`
projects endpoint methods into F buttons:

| Rule | Behavior |
| --- | --- |
| Endpoint detection | method has `@GetMapping/@PostMapping/@PutMapping/@DeleteMapping` |
| Button name | method `@ApiName` → class `@ApiName` → Java method name |
| Permission | method `@ApiPermission` → class `@ApiPermission`; `@ApiPublic` (method/class) → no perms (logged-in is enough) |
| Default-deny endpoints | neither `@ApiPermission` nor `@ApiPublic` (gateway 403) → **no button** |
| `@Hidden` | method/class level → skipped |
| menuId | permission string if present; Java method name for `@ApiPublic` |
| Order | declaration order, increasing |

```java
return ErpMenus.create()
    .dir("system", "System", "system", d -> d
        .menu("user", "Users", "user", ErpUserController.class) // buttons auto-generated
        .menu("group", "Groups", "lock").permsFrom(ErpGroupController.class))
    .menu("home", "Home", "home").component("erp/home");
```

## 5. Lifecycle & index rebuild

- **Register**: `McerpExpansion.onRegister()` (SoysExpansion skeleton callback) → writes the `ErpRegistry` index;
- **Unregister**: `onUnregister()` / plugin disable (`PluginDisableEvent`) → index removed;
- **Detection**: `instanceof McerpExpansion` (host `McErpHostExpansion` is a direct `SoysExpansion` subclass, naturally excluded);
- **Fallback rebuild**: `/mcerp reload`, `/soyshttp reload` (via `registerReloadHook`), and one scan after full server start
  all rebuild the index from `SoysExpansion.REGISTERED` (clean stale + re-register).

## 6. Hosting & micro-frontend

- Addon static assets go in `resources/dist/`; declared via `resourceRoot()` and hosted at `/web/plugins/<moduleID>/`;
- Menus default to **wujie micro-frontend** embedding (`wujie()` = true): the sub-app needs
  history routing + absolute publicPath + empty Layout (no own login page or sidebar);
- `wujie()` = false falls back to classic iframe.

> See 06-Frontend-and-Micro-Frontend.
