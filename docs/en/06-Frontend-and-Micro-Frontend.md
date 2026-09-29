# 06 · Frontend & Micro-Frontend

The frontend project lives in `RuoYi-Vue2-master/` (customized RuoYi-Vue2 3.9.2 / Vue 2.6 / element-ui 2.15 / vue-router 3).

## 1. Build & sync



```
cd RuoYi-Vue2-master

npm install          # first time

npm run build:prod   # production build (dist/)
```

Sync `dist/` **fully** into the backend resources, then repackage:



```
Copy-Item dist\\\* ..\src\main\resources\dist\ -Recurse -Force

cd ..

mvn -o clean package -DskipTests
```

> `dist/`
>
>  must contain 
>
> `__SOYS_CONTEXT__.js`
>
>  (contract injection point). Never hand-edit it.

## 2. Production config

`vue.config.js`:



```
publicPath: process.env.NODE\_ENV === "production" ? "/web/plugins/MCERP/" : "/",
```

`.env.production`:



```
VUE\_APP\_TITLE = MCERP

VUE\_APP\_BASE\_API = '/prod-api'
```

> `VUE_APP_BASE_API`
>
>  is the dev-proxy relative prefix; production API address is decided by the contract 
>
> `apiBase`
>
>  (
>
> `/api`
>
> ) at runtime.

## 3. Login & auth frontend



* Login/logout/session reuse the main plugin auth endpoints (`/api/auth/*`) and `soys-auth.js`:

  Bearer injection, unified 401 → login page;

* MCERP frontend contract:


  * `GET /api/plugins/MCERP/auth/getInfo` → user/roles/permissions;

  * `GET /api/plugins/MCERP/auth/getRouters` → visible route tree;

* `permission.js` builds the sidebar and routes dynamically from `getRouters`.

## 4. Micro-frontend (wujie) & iframe

### 4.1 Main app side (MCERP)



* Menus render by module `componentMode`:


  * `WUJIE` (default): `WujieLink` component — name = plugin id (keep-alive key), url = module page,

    props carry token and micro-mode flag;

  * `IFRAME`: classic iframe.

* TagsView keep-alive supports both modes (meta carries the mode marker).

* On menu switch (url change) the main app emits `router-push` via `$wujie.bus` so the sub-app

  navigates internally without reload.

### 4.2 Sub-app side (addon frontend, e.g. SOYSHTTPOverMC-ERP)

wujie mode requires:



| Item            | Requirement                                                                                                                                                               |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `publicPath`    | production build uses an **absolute prefix** (e.g. `/web/plugins/<moduleID>/`), else chunks 404 on deep URLs                                                              |
| `router base`   | history mode + `base` taken at runtime from the contract `pageFullPrefix`                                                                                                 |
| micro lifecycle | `__WUJIE_MOUNT__` / `__WUJIE_UNMOUNT__`: on mount read `props.path` and `router.push`; listen to `$wujie.bus` `router-push` for runtime sync; off the listener on unmount |
| Layout / login  | strip own Layout (empty Layout) and disable own login page (same-origin already logged in; main app does auth)                                                            |

### 4.3 Menu-switch route sync

In the main app `WujieLink/index.vue`, on url change:



```
this.\$nextTick(() => {

&#x20; this.\$refs.wujie.bus.\$emit('router-push', this.props.path)

})
```

`props.path` is the sub-app route path for the current menu (e.g. `/erp/group`).

If the sub-app stays on an old page after clicking a menu, check whether this emit is present.

### 4.4 element-ui popups inside the wujie sandbox

Inside the shadowRoot, element-ui popper positioning can break (e.g. dropdowns jumping to the top-left).

Fix direction: inject plugins (global CSS/JS) into the wujie config in `WujieLink` to fix popper anchoring.

## 5. Troubleshooting



| Symptom                                 | Cause / fix                                                                |
| --------------------------------------- | -------------------------------------------------------------------------- |
| Sub-app blank / chunk 404               | `publicPath` not absolute, or router `base` not set to `pageFullPrefix`    |
| Sub-app doesn't navigate on menu switch | main app not emitting `router-push`; check `WujieLink/index.vue`           |
| Popup/dropdown mispositioned            | wujie sandbox popper issue; inject fix CSS/JS or use non-popper components |
| Logo still RuoYi                        | `src/assets/logo/logo.png` not replaced; rebuild and sync dist             |