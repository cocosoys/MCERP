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

## 7. Plugin-supplied menu icons (tutorial)

The sidebar is rendered by the MCERP host, so an addon's own `SvgIcon` is never involved. Icon assets must be
directly reachable by the host: either a name baked into the host sprite, a same-origin image path, or a logical
key resolved through the plugin's `icons/manifest.json`. The walkthrough below goes resource → manifest → menu → check.

### 7.1 Which form to use

| Form | Example | Rendering | When |
| --- | --- | --- | --- |
| sprite name | `system`, `user` | `<svg><use href="#icon-…">`, monochrome, follows theme | Icons already bundled in the host |
| same-origin image path | `/web/plugins/stock/icons/box.png` | `<img>` in original colors, no mask tint | You already have a fixed png/svg path |
| `img:<key>` | `img:pack` | Looks up this plugin's `icons/manifest.json`, then renders as image | Many icons, or you want logical names decoupled from filenames |

Three hard rules:

- A bare name must exist under the host's `src/assets/icons/svg/`, otherwise the slot stays blank;
- An image path must be a same-origin path starting with a **single** `/`; extensions are limited to
  `png/jpg/jpeg/gif/webp/svg/ico/bmp/avif`, query strings allowed;
- `img:` is only a logical key. If it cannot be resolved the raw value is kept and the host falls back to the
  `form` icon, so the slot never goes blank.

### 7.2 Step 1 — ship the images

Put icons in the addon's `src/main/resources/dist/icons/`; they are hosted under
`/web/plugins/<moduleID>/icons/` via `resourceRoot()`. 16×16 or 32×32 png is a good size (the sidebar renders at
`1em` and pixel art gets `image-rendering: pixelated`).

```
dist/
├── icons/
│   ├── gift.png
│   ├── light_blue_bundle.png
│   └── manifest.json
└── index.html
```

### 7.3 Step 2 — publish `icons/manifest.json`

```json
{
  "version": 1,
  "icons": {
    "gift": "icons/gift.png",
    "pack": "icons/light_blue_bundle.png"
  }
}
```

- Values may be relative to the plugin root (recommended) or absolute `/web/plugins/<id>/...` paths;
- Values containing `..`, or absolute paths outside `/web/plugins/`, are rejected;
- `version` is currently informational only;
- The host probes a manifest only when a subtree actually uses `img:`; sprite names or direct image paths cause
  zero requests, so login latency is unchanged.

### 7.4 Step 3 — reference it from the menu

The third argument of `dir/menu/route` in `ErpMenus` is the icon:

```java
@Override
protected ErpMenus menus() {
    return ErpMenus.create()
        .dir("loot", "Loot Config", "img:gift", d -> d
            .menu("reward", "Rewards", "img:pack")
            .menu("monster", "Monsters", "bug")
            .menu("claim", "Claims", "form"))
        .menu("config", "Plugin Config", "edit");
}
```

The module-level icon uses the `icon()` hook (defaults to `null`, which the host turns into `link`, not a blank slot):

```java
@Override
protected String icon() {
    return "img:gift";
}
```

### 7.5 How the host resolves it

1. After login the app calls `GET /auth/getRouters` and receives the menu tree;
2. `store/modules/permission.js` calls `resolveMenuIcons` before `filterAsyncRouter`;
3. `utils/pluginIcons.js` collects only the plugins whose subtree contains `img:`, then fetches
   `/web/plugins/<id>/icons/manifest.json` in parallel (`credentials: same-origin`, `cache: force-cache`,
   3 s timeout, failures silent, results cached in two maps);
4. Ownership: the node `component` (`wujie:/web/plugins/<id>/…` or `iframe:…`) wins; a parent without a
   component uses the single plugin id found in its subtree;
5. On a hit, `meta.icon` is rewritten in place to `/web/plugins/<id>/icons/xxx.png` before route filtering;
6. `SvgIcon` renders `<img>` when `isImage` matches, otherwise the sprite; an image `error` resets
   `effectiveIcon` to `form`.

### 7.6 Self-check

```bash
# 1) manifest is reachable and is JSON (the static server has no HEAD support — use GET)
curl -s -o /dev/null -w "%{http_code} %{content_type}\n" \
  https://<host>/web/plugins/<moduleID>/icons/manifest.json

# 2) the image itself is reachable
curl -s -o /dev/null -w "%{http_code} %{content_type} %{size_download}\n" \
  https://<host>/web/plugins/<moduleID>/icons/light_blue_bundle.png
```

- Both should return `200`; icon responses usually carry `Cache-Control: public, max-age=300`;
- After login the menu value should already be a `/web/plugins/...` image URL;
- If a new image does not show up, hard-reload (`Ctrl+F5`) or use a new filename; the manifest is also cached
  until its max-age expires;
- A blank slot with healthy endpoints almost always means a missing sprite name or a path outside the whitelist.

### 7.7 Troubleshooting

| Symptom | Cause | Fix |
| --- | --- | --- |
| Icon slot blank | Name missing from host sprite, or path outside the whitelist | Use `img:<key>` or a compliant same-origin path |
| `img:` never resolves | Manifest 404, no `icons` object, or a wrong key | Run the curl checks; expect `{"icons":{...}}` |
| Image shows but tinted | It went through the sprite mask branch | Use an image path or `img:`, not a bare name |
| Blurry icon | Asset size does not match `1em` | Provide 16×16 / 32×32 art |
| Icon persists after uninstall | Browser cached the image | Expected; rename the file or clear the cache |

> Permissions: whether a menu appears also depends on `hasPermission` for the current credential. OP matching is
> now case-insensitive; permission groups for non-OP users are still resolved by the SOYS core via UUID — see the
> troubleshooting table in 07-Logging-and-Operations.
