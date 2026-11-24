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

* On menu switch (url change) the main app emits `router-push` via the **wujie global `bus`**
  (`import { bus as wujieBus } from 'wujie'`) so the sub-app navigates internally without reload; see 4.3.

### 4.2 Sub-app side (addon frontend, e.g. SOYSHTTPOverMC-ERP)

wujie mode requires:



| Item            | Requirement                                                                                                                                                               |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `publicPath`    | production build uses an **absolute prefix** (e.g. `/web/plugins/<moduleID>/`), else chunks 404 on deep URLs                                                              |
| `router base`   | history mode + `base` taken at runtime from the contract `pageFullPrefix`                                                                                                 |
| micro lifecycle | `__WUJIE_MOUNT__` / `__WUJIE_UNMOUNT__`: on mount read `props.path` and `router.push`; listen to the wujie global `bus` `router-push` for runtime sync; off the listener on unmount |
| Layout / login  | strip own Layout (empty Layout) and disable own login page (same-origin already logged in; main app does auth)                                                            |

### 4.3 Menu-switch route sync

In the main app `WujieLink/index.vue`, on url change:



```
import { bus as wujieBus } from 'wujie'

// inside $nextTick after the url changed
wujieBus.$emit('router-push', this.subPath)
```

Key points:

* Use the **wujie global `bus`** (`import { bus } from 'wujie'`), never `this.$refs.wujie.bus`:
  each wujie instance owns its own `bus` object, while the sub-app registers its listener once
  during `__WUJIE_MOUNT__`; an emit from `$refs.wujie.bus` never reaches that listener, which
  shows up as "the sub-app stays on the old page after clicking a menu".
* Emit `this.subPath` (the menu path relative to its module, e.g. `/config/gateway/gw-config`),
  not `this.props.path`; `props.path` is only used for the first `router.push` on mount.
* If the sub-app stays on an old page after clicking a menu, check whether this emit exists and
  whether `$refs.wujie.bus` was used by mistake.

### 4.4 element-ui popups inside the wujie sandbox

Inside the shadowRoot, element-ui popper positioning can break (e.g. dropdowns jumping to the top-left).

Fix direction: inject plugins (global CSS/JS) into the wujie config in `WujieLink` to fix popper anchoring.

### 4.5 Turning wujie URL sync off (`:sync="false"`)

The wujie config in `WujieLink` explicitly disables `sync`:

* **Symptom**: with `sync=true`, wujie writes the sub-app path into the main app query string keyed by
  sub-app name (`?SOYSGiftLoft=/web/plugins/SOYSGiftLoft/...`). Every menu switch appends another key,
  so switching between addon modules grows the URL unboundedly and pollutes browser history.
* **Decision**: `sync` is redundant in this architecture — the clean main-app URL already identifies the
  current menu; the sub-app is restored on mount by `props.path` and on menu switch by `router-push`
  from the global `bus`; a refresh resolves back to the same page through the parent route + `props.path`.
* **Cost**: temporary jumps made inside a sub-app but outside the menu tree (e.g. a detail page that is
  not registered as a menu) no longer appear in the address bar, and a refresh returns to that menu's
  default page. All sub-pages are registered as menus today, so this has no practical impact.

### 4.6 Avatar upload (`POST /system/user/profile/avatar`)

* `views/system/user/profile/userAvatar.vue` crops with vue-cropper and submits a **base64 data URL**
  in JSON (`AvatarUploadVO#img`); multipart is not used because the framework does not parse
  `multipart/form-data`.
* Backend `ErpUserServiceImpl#avatar` decodes the data URL, validates the image type, writes the file
  under `plugins/MCERP/avatar/`, registers it for static hosting
  (`/web/plugins/MCERP/avatar/<fileName>`) and stores a root-relative path in `erp_user.avatar`.
  The `AjaxResult` carries an extra top-level `imgUrl` so the UI can refresh immediately.
* On startup, `registerAvatarDirectory` bulk-registers the existing avatar directory.

### 4.7 Menu icons (sprite / image path / plugin manifest)

The sidebar is rendered by the MCERP host itself (`layout/components/Sidebar/Item.vue` → global
`SvgIcon`), so the rendering capability is defined by the host, not by each sub-app's own `SvgIcon`.

> Step-by-step integration (assets → manifest → menu reference → self-check) lives in 04-Extension-Development §7.

Three accepted forms:

| Form | Rendering | Constraint |
| --- | --- | --- |
| `user`, `form` (plain name) | `<svg><use xlink:href="#icon-…">` | the name must exist in `src/assets/icons/svg`, otherwise blank |
| `/web/plugins/SOYSGiftLoft/icons/light_blue_bundle.png` | `<img>`, original colours, no mask tint | same-origin path starting with a single `/`; extension must be png/jpg/jpeg/gif/webp/svg/ico/bmp/avif; a query string is allowed |
| `img:<key>` | resolved against the owning plugin's `icons/manifest.json`, then rendered as an image | falls back to `form` when unresolved or the image 404s |

Manifest contract (published by the plugin at its static root, i.e. the `dist/` root; the host fetches
it per plugin during `getRouters` after login, in parallel, with a 3 s timeout and silent failure):

```json
{
  "version": 1,
  "icons": {
    "pack": "icons/light_blue_bundle.png"
  }
}
```

* The value may be relative to the plugin root, or an absolute `/web/plugins/<id>/...` path. Paths
  containing `..`, or absolute paths outside `/web/plugins/`, are rejected.
* `img:` ownership is derived from the menu node's `component` (`wujie:/web/plugins/<id>/…` or
  `iframe:…`); a parent node without a component inherits the single plugin found in its subtree.
* Resolution lives in `utils/pluginIcons.js`; it is hooked into `GenerateRoutes` in
  `store/modules/permission.js`.
* Only plugins whose subtree actually uses `img:` are probed for a manifest; when every menu uses a
  sprite name or a direct image path, no request is made and login latency is unchanged.
* The static server does **not** support HEAD — probe with GET only. Icon responses carry
  `Cache-Control: public, max-age=300`.
* When a plugin is removed or a path breaks, the image `error` handler falls back instead of leaving a
  blank slot.


## 5. Troubleshooting



| Symptom                                 | Cause / fix                                                                |
| --------------------------------------- | -------------------------------------------------------------------------- |
| Sub-app blank / chunk 404               | `publicPath` not absolute, or router `base` not set to `pageFullPrefix`    |
| Sub-app doesn't navigate on menu switch | main app not emitting `router-push`; check `WujieLink/index.vue`           |
| Popup/dropdown mispositioned            | wujie sandbox popper issue; inject fix CSS/JS or use non-popper components |
| Logo still RuoYi                        | `src/assets/logo/logo.png` not replaced; rebuild and sync dist             |
| Menu icon blank                         | name missing from host sprite; image path outside the whitelist (single `/` + image extension); or plugin `icons/manifest.json` 404 |