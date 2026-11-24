# 07 · Logging & Operations

This page explains MCERP's oper-log and login-log mechanisms plus common operational practices.

## 1. Oper logs

### 1.1 Recorded fields

Each `erp_oper_log` row:

| Field | Source |
| --- | --- |
| `title` | Business module (e.g. "User management") |
| `businessType` | Derived from action text: 1 add / 2 edit / 3 delete / 0 other |
| `method` | Business action (e.g. "Add user") |
| `requestMethod` | HTTP method, from request context |
| `operName` | Operator player name (SOYS credential) |
| `operUrl` | Request URL with prefix (e.g. `/api/plugins/MCERP/system/user`) |
| `operIp` | Client IP |
| `operParam` | Target object name (e.g. added username) |
| `jsonResult` | Result message (e.g. "Add succeeded") |
| `status` / `errorMsg` | Status (0 ok) / error |
| `operTime` | Operation time (yyyy-MM-dd HH:mm:ss) |

### 1.2 Mechanism

- Entry: `OperLogService.record(credential, title, business, target, result)`, called by each
  `*ServiceImpl` after a successful business action (~20 call sites);
- **Request context**: the main plugin's `ApiRegistry.dispatch` binds `ApiRequestContext` to the current
  worker thread (`ApiRequestContext.bind(ctx)`); `OperLogServiceImpl` reads
  `requestMethod / operUrl / operIp` via `ApiRequestContext.current()` — no signature changes at call sites;
- Write failures only warn; they never block business (cross-cutting logging).

## 2. Login logs

### 2.1 Recorded fields

Each `erp_logininfor` row:

| Field | Notes |
| --- | --- |
| `userName` | Player name |
| `ipaddr` | Client IP |
| `status` | `0` success / `1` failure |
| `msg` | Result text (success: "Login succeeded"; failure includes reason, e.g. "Account or password error…") |
| `loginTime` | Login time |

### 2.2 Mechanism

- `LoginLogListener` (Bukkit listener) does sidecar bookkeeping on main plugin gateway events:
  - `GatewayLoginResultEvent` (**main channel**): the main plugin fires it from `AuthServiceImpl.login()`
    (success + all failure branches) and `checkStatus()` (remember-me / game-IP auto-login success),
    carrying player / success / reason / ip → MCERP writes success (status=0) or failure (status=1);
  - `GatewayCredentialIssuedEvent`: `/soyshttp key` credential issuance → success record (fallback);
  - `GatewayAccessDeniedEvent`: only `/auth/*` gateway-policy denials → failure record (fallback).
- Login itself is fully delegated to the main plugin; this listener only records, and write failures warn without affecting the gateway.

### 2.3 Frontend

- "Log management → Oper log": list + detail (method/URL/IP/result);
- "Log management → Login log": success/failure status, reason, IP.

### 2.4 Export (CSV)

- Endpoints: `GET /api/plugins/MCERP/monitor/operlog/export` and
  `GET /api/plugins/MCERP/monitor/logininfor/export`, guarded by
  `mcerp:monitor:operlog:export` / `mcerp:monitor:logininfor:export`.
- Filters match the list endpoints exactly (`title` / `operName` for oper log, `userName` /
  `status` for login log), newest first, capped at 10,000 rows per export.
- Annotated APIs always return a JSON envelope and have no binary channel, so the endpoint returns
  `{fileName, content, total}` and the frontend assembles a Blob (adding a UTF-8 BOM so Excel
  renders Chinese correctly); the downloaded file is `.csv`, not `.xlsx`.

## 3. Operational commands

| Command | Purpose |
| --- | --- |
| `/mcerp reload` | Rebuild ERP module index from `SoysExpansion.REGISTERED` (clean stale + re-register) |
| `/mcerp list` | List registered modules (id + display name) |
| `/soyshttp reload` | Main plugin reload (triggers `registerReloadHook`, MCERP rebuilds the index too) |

## 4. Build & deploy cheat sheet

```powershell
# backend
cd .\MCERP-master
mvn -o clean package -DskipTests
Copy-Item target\MCERP-1.0-SNAPSHOT.jar <server dir>\plugins\ -Force

# after frontend changes
cd RuoYi-Vue2-master
npm run build:prod
Copy-Item dist\* ..\src\main\resources\dist\ -Recurse -Force
cd .. && mvn -o clean package -DskipTests
```

> After changing the **main plugin** (SOYSHTTPOverMC), rebuild its jar first
> (`core/target/SOYSHTTPOverMC-1_12-1.4.0.jar`), replace it under the server `plugins/`,
> then rebuild MCERP (its `pom.xml` systemPath references the main plugin jar).

## 5. Troubleshooting

| Symptom | Investigation |
| --- | --- |
| Oper-log detail fields empty | Main plugin jar must include `ApiRequestContext.current()` (the ≥1.4.0 change); request must go through `ApiRegistry.dispatch` |
| No login-log success/failure | Main plugin must fire `GatewayLoginResultEvent` (wired in `AuthServiceImpl.login`); `LoginLogListener` must be registered |
| Log write failures | Check `latest.log` for `mcerp.operlog.write-failed` / `mcerp.logininfor.write-failed` warnings |
| Module registration issues | `/mcerp list` to see registered modules; `/mcerp reload` to rebuild |
| Only a few modules after login, role shows `common` | Login name differs in case from the OP name in `ops.json`: on offline-mode servers the UUID is derived from `OfflinePlayer:<name>`, so `Admin` and `admin` are different UUIDs. `AuthServiceImpl.isOp` now matches OP names case-insensitively; if it still fails, compare `ops.json` `name` with the login name |
