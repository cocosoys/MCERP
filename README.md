# MCERP

MCERP 是一个运行于 Spigot 1.12.2 之上的 Minecraft 服务器 ERP 后台中控插件，整体挂载在 SOYSHTTPOverMC（HTTP-Over-MC 网关）之上：**前端复用 RuoYi-Vue2 后台界面，后端完全基于 SOYS 注解式 API**，登录、凭证、鉴权、路由、页面托管全部由主插件统一承担。

> English: MCERP is an ERP dashboard plugin for Spigot 1.12.2 servers, mounted on the SOYSHTTPOverMC HTTP-over-MC gateway. It reuses a RuoYi-Vue2 dashboard UI while all backend APIs, auth, routing and static hosting are provided by the main plugin SOYSHTTPOverMC. Documentation: [中文教程](docs/zh_cn/01-快速开始.md) · [English Tutorial](docs/en/01-Quick-Start.md)

## 特性

- **零配置可迁移部署**：前端契约文件 `__SOYS_CONTEXT__.js` 由主插件动态注入，`apiBase`、页面前缀、host/port 全部运行时自适应——把插件连同前端复制到任意服务器即可使用，前端代码中不再有写死的地址。
- **统一中控 + 附属 ERP 模块**：`McerpExpansion`（继承主插件 `SoysExpansion`）一行 `register()` 即完成 API 注册、页面托管、菜单/路由登记，即安即生效；`/mcerp reload` 与 `/soyshttp reload` 双通道重建索引。
- **声明式菜单 DSL**：`ErpMenus.create()` 以类 YAML 的 `dir/menu/perm/route` 链式声明菜单树；指定 controller 类即可自动把端点投影为按钮权限，免手写。
- **鉴权完全委托**：登录/登出/验证码/会话全部走主插件 auth（`/api/auth/*`），权限基于主插件权限镜像（OP 默认全权限）。
- **双存储模式**：实体注解驱动，YAML（`data/*.yml`）与 SQL（`mcerp-init.sql`，MySQL/SQLite）两种初始化方式，8 张业务表主键统一 `AUTO_INCREMENT`。
- **完整日志链路**：操作日志记录请求方法/地址/客户端 IP/返回结果；登录日志记录成功/失败状态与原因（基于主插件 `GatewayLoginResultEvent`）。
- **微前端渲染**：附属模块页面默认以 wujie（无界微前端）模式嵌入主应用，菜单切换不重载、标签页保活。

## 快速开始

前置要求：Spigot 1.12.2 服务端 + [SOYSHTTPOverMC](https://github.com/cocosoys)（版本 ≥ 1.4.0）。

1. 构建后端：`mvn -o clean package -DskipTests`（产物 `target/MCERP-1.0-SNAPSHOT.jar`）；
2. 复制 jar 到服务端 `plugins/`，重启服务器；
3. 浏览器访问 `https://<host>:<port>/web/plugins/MCERP/index`（默认测试服 `https://localhost:25565/web/plugins/MCERP/index`）。

详细步骤见 [docs/zh_cn/01-快速开始.md](docs/zh_cn/01-快速开始.md) / [docs/en/01-Quick-Start.md](docs/en/01-Quick-Start.md)。

## 文档

| 主题 | 中文 | English |
| --- | --- | --- |
| 快速开始 | [01-快速开始](docs/zh_cn/01-快速开始.md) | [01-Quick-Start](docs/en/01-Quick-Start.md) |
| 架构与契约 | [02-架构与契约](docs/zh_cn/02-架构与契约.md) | [02-Architecture-and-Contract](docs/en/02-Architecture-and-Contract.md) |
| 核心功能 | [03-核心功能](docs/zh_cn/03-核心功能.md) | [03-Core-Features](docs/en/03-Core-Features.md) |
| 扩展开发（附属 ERP 模块） | [04-扩展开发](docs/zh_cn/04-扩展开发.md) | [04-Extension-Development](docs/en/04-Extension-Development.md) |
| 数据存储与初始化 | [05-数据与初始化](docs/zh_cn/05-数据与初始化.md) | [05-Data-and-Initialization](docs/en/05-Data-and-Initialization.md) |
| 前端定制与微前端 | [06-前端定制与微前端](docs/zh_cn/06-前端定制与微前端.md) | [06-Frontend-and-Micro-Frontend](docs/en/06-Frontend-and-Micro-Frontend.md) |
| 日志与运维 | [07-日志与运维](docs/zh_cn/07-日志与运维.md) | [07-Logging-and-Operations](docs/en/07-Logging-and-Operations.md) |

## 命令

```
/mcerp reload   # 重建 ERP 模块索引（以主插件 SoysExpansion.REGISTERED 为准，清理残留）
/mcerp list     # 列出当前已登记的 ERP 模块
```
别名：`/erp`。需要权限 `mcerp.admin`（默认 OP）。

## 目录结构

```
MCERP/
├── src/main/java/com/github/cocosoys/mc/mcerp/
│   ├── MCERP.java                 # 插件入口（onEnable/指令/事件）
│   ├── McErpHostExpansion.java    # 宿主门户（继承 SoysExpansion，注册本插件全部 Controller + dist 托管）
│   ├── McerpExpansion.java        # 附属 ERP 模块扩展基类（继承 SoysExpansion）
│   ├── ErpRegistry.java           # ERP 模块索引（identifier 投影，实时反查实例）
│   ├── ErpMenus.java              # 菜单声明式 DSL 构建器
│   ├── ControllerPermScanner.java # Controller 端点 → 按钮权限自动扫描器
│   ├── controller/                # 8 个控制器（薄层，零逻辑）
│   ├── service/ + impl/           # Service 抽象层 + 实现层（事务与业务逻辑）
│   ├── entity/ + entity/vo/       # 实体（与表同名）+ VO（表字段属性）
│   ├── listener/LoginLogListener  # 登录日志旁路记账（主插件网关事件）
│   ├── i18n/McerpI18n.java        # i18n 门面（注册语言包到主插件）
│   └── util/                       # Ids / NameSafe 工具类
├── src/main/resources/
│   ├── plugin.yml                 # 插件清单（权限声明）
│   ├── data/*.yml                 # YAML 存储模式初始化数据（8 表）
│   ├── sql/mcerp-init.sql         # SQL 存储模式初始化脚本
│   ├── language/zh_cn.yml         # 中文语言包
│   └── dist/                      # 前端构建产物（含 __SOYS_CONTEXT__.js 契约）
├── RuoYi-Vue2-master/             # 前端工程（npm run build:prod → dist 同步）
└── pom.xml                        # Maven 构建（依赖主插件 1.4.0 jar）
```

## 相关项目

- [SOYSHTTPOverMC](https://github.com/cocosoys) —— HTTP-Over-MC 网关主插件，提供注解式 API、auth、页面托管、`SoysExpansion` 扩展骨架。

## 许可证

MIT © cocosoys
