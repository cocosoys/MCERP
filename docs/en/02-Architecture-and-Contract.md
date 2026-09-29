# 02 · Architecture and Contract

This page explains the overall architecture, request flow, the frontend contract (`__SOYS_CONTEXT__.js`) and the auth/permission model.

## 1. Architecture

```
Browser
  │  https://<host>:<port>/web/plugins/MCERP/index
  ▼
SOYSHTTPOverMC gateway (HTTP-Over-MC tunnel, Spigot 1.12.2)
  ├─ Annotation-based API registry (ApiRegistry)
  │    └─ /api/plugins/MCERP/prod-api/*   → MCERP controllers
  │    └─ /api/plugins/<moduleID>/*        → addon ERP module controllers
  ├─ Static hosting (/web/plugins/*)
  │    └─ dist/ frontend + __SOYS_CONTEXT__.js contract injection
  ├─ Auth flow (/api/auth/login|logout|me|status|mode)
  ├─ Permission mirror (Bukkit permissions → API permissions)
  └─ Event bus (GatewayLoginResultEvent / GatewayCredentialIssuedEvent / ...)
       └─ MCERP sidecar listeners: login logs, module index rebuild
```

- **MCERP is a projector of the main plugin's `SoysExpansion.REGISTERED`**: it does not manage
  module lifecycles itself. It keeps a lightweight index (identifier → owner plugin name) and
  assembles module content (menus, routes) **live** from REGISTERED instances at composition time —
  one source of truth, no cache staleness.
- **Addon ERP modules** are independent plugins extending `McerpExpansion`
  (see [04-Extension-Development](04-Extension-Development.md)), recognized via `instanceof McerpExpansion`.

## 2. Request flow (example: add user)

```
Frontend (RuoYi-Vue2, inside dist)
  POST /api/plugins/MCERP/system/user     ← apiBase from contract, no hard-coding
      │  Authorization: Bearer <token>
      ▼
SOYS gateway ApiRegistry.dispatch()
  ├─ resolve credential (Bearer/Cookie/X-API-Key)
  ├─ ApiRequestContext.bind(ctx)          ← lets oper-log read request info without signature changes
  ├─ permission check (@ApiPermission / @ApiPublic / default-deny)
  ▼
ErpUserController.add()   (thin layer, zero logic)
  ▼
ErpUserService.add()      (abstract layer)
  ▼
ErpUserServiceImpl.add()  (impl layer: transaction + logic + oper-log record)
  ▼
DATA.insert(ErpUser)      → erp_user.yml / erp_user table
```

## 3. Frontend contract (`__SOYS_CONTEXT__.js`)

The main plugin ContractInjector replaces placeholders in the served `dist/index.html`
with server environment primitives:

```js
// At serve time __SOYS_CONTEXT__ becomes:
// { "scheme":"http|https", "host":"...", "port":8080,
//   "apiPrefix":"/api", "pluginsPrefix":"/plugins/MCER",
//   "fullPrefix":"/api/plugins/MCER",
//   "pagePrefix":"/plugins/MCER", "webResourcePrefix":"web/plugins/MCER/page/" }
window.SOYS_CONTEXT = typeof __SOYS_CONTEXT__ !== 'undefined' ? __SOYS_CONTEXT__ : null;

// HTML resource rewrite exclusions: /prod-api is the app API prefix, /api is SOYS reserved
window.SOYS_CONTEXT_EXCLUDES = ["/prod-api", "/api"];
```

**Why it matters**: copy the plugin + frontend to another server and everything still works —
apiBase (`/api`), page prefix (`/web/plugins/MCERP`), host/port are all adapted at runtime.

Production build key config (`vue.config.js`):

```js
publicPath: process.env.NODE_ENV === "production" ? "/web/plugins/MCERP/" : "/",
```

> `publicPath` only writes the server-relative absolute path (no host/port); host/port are supplied by the contract at runtime.

## 4. Auth and permission model

**MCERP does not implement auth itself**; login/logout/captcha/session are fully delegated to the main plugin:

| Endpoint | Purpose |
| --- | --- |
| `POST /api/auth/login` | Popup login (AuthMe password check or password-free) |
| `POST /api/auth/issue` | Ticket login (in-game link) |
| `GET /api/auth/status` | Session status (remember-me device auto-login / game-IP auto-login) |
| `GET /api/auth/me` / `POST /api/auth/logout` | Current user / logout |

- The frontend reuses the main plugin's `soys-auth.js` (Bearer injection, 401 → login page).
- MCERP keeps only two RuoYi-contract read endpoints:
  - `GET /api/plugins/MCERP/auth/getInfo`: current user (user/roles/permissions);
  - `GET /api/plugins/MCERP/auth/getRouters`: visible route tree (menu table ⊕ registered modules).

**Permission model**:
- API permission = main plugin permission mirror: `@ApiPermission("mcerp:xx")` requires the matching Bukkit permission;
- **OP gets everything** (the main plugin permission store allows OP through);
- Menu visibility is decided by the `getRouters` route tree, filtered by the caller's permissions.

## 5. Prefix quick reference

| Concept | Value |
| --- | --- |
| API prefix | `/api` (main plugin apiPrefix) |
| MCERP API root | `/api/plugins/MCERP` |
| Page hosting root | `/web/plugins/MCERP` |
| Addon page root | `/web/plugins/<moduleID>` (`pageRoot()`) |
| Menu component default prefix | `/web/plugins/<moduleID>/<safeDirName>` (`pageDirRoot()`, identifier filtered + lowercased) |
| Data dir | `plugins/SOYSHTTPOverMC/data/*.yml` (YAML mode) |

> `NameSafe.path()`: keeps letters/digits/underscore and lowercases, e.g. `SOYSHTTPOverMC-ERP` → `soyshttpovermcerp`.
