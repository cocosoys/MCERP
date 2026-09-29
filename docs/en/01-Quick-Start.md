# 01 · Quick Start

This guide covers deploying MCERP from scratch: requirements, backend build, frontend build, deployment and access.

## 1. Requirements

| Item | Requirement |
| --- | --- |
| Server | Spigot 1.12.2 (CraftBukkit 1.12.2-R0.1-SNAPSHOT) |
| Main plugin | SOYSHTTPOverMC ≥ 1.4.0 (`depend: [SOYSHTTPOverMC]`, must be present in `plugins/` first) |
| JDK | 8 |
| Build | Maven 3.x (backend); Node.js + npm (frontend, optional) |

> MCERP references the main plugin artifact directly via `systemPath` in `pom.xml`:
> `../SOYSHTTPOverMC/core/target/SOYSHTTPOverMC-1_12-1.4.0.jar`.
> So the main plugin sources must live beside MCERP (both under `plugins/spigot/1.12.2/`), and the main plugin jar must be built.

## 2. Backend build

```powershell
cd D:\WorkTools\Project\Minecraft\plugins\spigot\1.12.2\MCERP
mvn -o clean package -DskipTests
```

> ⚠️ Always use `clean`. Maven incremental compilation may reuse stale broken classes
> (e.g. `log cannot be resolved` at runtime). `clean` forces a full rebuild.

Output: `target/MCERP-1.0-SNAPSHOT.jar`.

## 3. Frontend build (only after frontend changes)

Frontend project lives in `RuoYi-Vue2-master/` (customized RuoYi-Vue2):

```powershell
cd RuoYi-Vue2-master
npm install          # first time
npm run build:prod
```

Sync the `dist/` output **fully** into the backend resources:

```powershell
Copy-Item dist\* ..\src\main\resources\dist\ -Recurse -Force
```

Then rerun step 2 to package the new frontend into the jar.

> `dist/` must contain `__SOYS_CONTEXT__.js` — the main plugin ContractInjector contract file.
> At serve time it replaces apiBase / page prefix / host / port placeholders with server primitives,
> so the frontend code never hard-codes any address.

## 4. Deploy

1. Copy the jar into the server plugin folder:

   ```powershell
   Copy-Item target\MCERP-1.0-SNAPSHOT.jar D:\MyGame\Minecraft\Server\Spigot\1.12.2\plugins\ -Force
   ```

2. Restart the server (stop the old process, then start again):

   ```powershell
   Get-NetTCPConnection -LocalPort 25565 -State Listen | Stop-Process -Id { $_.OwningProcess } -Force
   Start-Process D:\WorkTools\JDK\8\bin\java.exe -WorkingDirectory D:\MyGame\Minecraft\Server\Spigot\1.12.2 `
     -ArgumentList "-Xmx2G","-jar","Spigot-1.12.2-build1573k.jar","nogui"
   ```

3. Verify: port 25565 listens, `logs/latest.log` shows:

   ```
   [MCERP] SOYS 接入完成：SoysExpansion 门户注册（路由） + dist 托管（expansion:MCERP）
   [MCERP] 已启用 (ERP 统一中控)
   [MCERP] ERP 模块已登记: <addon identifier>
   ```

## 5. Access

- Test entry: `https://localhost:25565/web/plugins/MCERP/index`
- After moving servers: `https://<host>:<port>/web/plugins/MCERP/index`

> The server uses a self-signed HTTPS certificate; accept the browser warning.

Login is fully delegated to the main plugin auth (`/api/auth/login`):
- Without AuthMe: enter any valid player name (letters/digits/underscore, ≤16 chars) — password-free;
- With AuthMe: player name + AuthMe password.

## 6. Commands

| Command | Description |
| --- | --- |
| `/mcerp reload` (alias `/erp reload`) | Rebuild the ERP module index from `SoysExpansion.REGISTERED` (clean stale + re-register) |
| `/mcerp list` (alias `/erp list`) | List registered ERP modules (id + display name) |

Requires `mcerp.admin` (default OP).

## 7. Troubleshooting

| Symptom | Cause / fix |
| --- | --- |
| Page 404 | Frontend not packaged, or `dist/` missing `__SOYS_CONTEXT__.js`; rebuild frontend and sync dist |
| API 404 | Check full prefix `/api/plugins/MCERP/...`; routes have no trailing slash (`/system/user`, not `/system/user/`) |
| Forced to login page | Main plugin session-token issuer disabled; check `gateway/issuers/session-token.yml` `enabled: true` |
| Page blank, `Maximum call stack size exceeded` | Frontend dist mismatched with backend contract (usually stale frontend); rebuild and sync |
