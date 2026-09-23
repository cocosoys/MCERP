package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.entity.vo.ErpMenuVO;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * ERP 菜单/路由声明式构建器（类 YAML 嵌套）。
 *
 * <p>由 {@link McerpExpansion#menus()} 返回（menusTable() 直接返回 {@link List}&lt;{@link ErpMenuVO}&gt;，
 * 不经本构建器），覆写者用 {@code dir/menu/perm/route} 链式声明，层级由 lambda 缩进（或 {@link #up()}）表达。
 * 全部属性直接使用 {@link ErpMenuVO}（继承 {@link com.github.cocosoys.mc.mcerp.entity.ErpMenu}）
 * 的表字段名：menuId / menuName / icon / menuType / path / component / perms / visible / orderNum 等。
 *
 * <pre>{@code
 * @Override protected ErpMenus menus() {
 *     return ErpMenus.create()
 *         .dir("system", "系统管理", "system", d -> d        // M 目录，lambda 内为子级
 *             .menu("user", "用户列表", "user")              // C 菜单（叶子页面）
 *                 .perm("soys.erp.user.list")               // F 按钮（挂到当前层级，与 menu 平级）
 *                 .perm("soys.erp.user.add")
 *             .menu("group", "权限组列表", "lock"))
 *         .menu("home", "首页", "home").component("erp/home"); // 顶层叶子
 * }
 * }</pre>
 *
 * <p>全属性写法（spec 一次填齐，不依赖隐式状态）：</p>
 *
 * <pre>{@code
 * return ErpMenus.create()
 *     .dir(s -> s.menuId("system").menuName("系统管理").icon("system").orderNum(1), d -> d
 *         .menu(m -> m.menuId("user").menuName("用户列表").icon("user")
 *             .perms("soys.erp.user.list").orderNum(10).component("erp/user").visible(true))
 *         .perm(p -> p.menuId("user:add").menuName("新增用户").perms("soys.erp.user.add")))
 *     .route("stock/io", "erp/io").perms("stock:io:list");
 * }</pre>
 *
 * <p>两种层级表达可混用：
 * <ul>
 *   <li><b>lambda 嵌套</b>：{@code dir(menuId,menuName,icon, sub)}，sub 内声明的一切挂到该目录下；</li>
 *   <li><b>流式 + up()</b>：{@code dir(menuId,menuName,icon)} 进入子级上下文，后续 add 挂入，{@link #up()} 返回上一级。</li>
 * </ul>
 *
 * <p><b>自动按钮</b>（免手写 perm）：menu 声明时可带 controller 类（或链式
 * {@code permsFrom(Class...)})，自动把该 controller 的端点方法（{@code @GetMapping/@PostMapping/...}）
 * 按 {@code @ApiName/@ApiPermission/@ApiPublic/@Hidden} 投影为 F 按钮挂到该菜单下，
 * 规则详见 {@link ControllerPermScanner}：</p>
 *
 * <pre>{@code
 * @Override protected ErpMenus menus() {
 *     return ErpMenus.create()
 *         .dir("system", "系统管理", "system", d -> d
 *             .menu("user", "用户列表", "user", ErpUserController.class) // 按钮自动生成
 *             .menu("group", "权限组列表", "lock").permsFrom(ErpGroupController.class))
 *         .menu("home", "首页", "home").component("erp/home");
 * }
 * }</pre>
 *
 * <p>方法名定节点类型：{@code dir}=M 目录、{@code menu}=C 菜单、{@code perm}=F 按钮权限、
 * {@code route}=path→component 便捷 C 菜单。属性两种填法：
 * <ul>
 *   <li><b>精简位置参数</b>：核心参数按位置（menuId、menuName、icon），其余（perms/orderNum/component/visible/path）
 *       用链式 setter 修饰<b>最近声明</b>的节点；lambda 结束后 setter 作用于目录本身（dir）而非其子级；</li>
 *   <li><b>全属性 spec</b>：{@code dir(Consumer&lt;MenuSpec&gt;, sub)} / {@code menu(Consumer&lt;MenuSpec&gt;)} /
 *       {@code perm(Consumer&lt;MenuSpec&gt;)}，spec 内所有表字段（menuId/menuName/icon/path/component/perms/visible/orderNum/...）一次填齐，
 *       不依赖隐式状态。</li>
 * </ul>
 *
 * <p>构建器无共享状态：每次覆写调用应新建（{@link #create()}），
 * {@link #build()} 返回根节点列表快照，可重复调用。
 */
public final class ErpMenus {

    /** 根节点列表（当前栈为空时 attach 的节点落在根部）。 */
    private final List<ErpMenuVO> roots = new ArrayList<>();

    /** 层级栈：dir 进入子级上下文时压栈，{@link #up()} / lambda 结束弹栈；栈顶即当前父节点。 */
    private final Deque<ErpMenuVO> stack = new ArrayDeque<>();

    /** 最近一次声明/attach 的节点（链式 setter 的作用目标）。 */
    private ErpMenuVO last;

    /** 私有构造器：统一经 {@link #create()} 创建（构建器无共享状态）。 */
    private ErpMenus() {
    }

    /** 新建构建器。 */
    public static ErpMenus create() {
        return new ErpMenus();
    }

    // ===== 节点声明（方法名定 menuType）=====

    /** M 目录 + lambda 子级（推荐，最接近 YAML 缩进）。 */
    public ErpMenus dir(String menuId, String menuName, String icon, Consumer<ErpMenus> sub) {
        ErpMenuVO node = node(menuId, menuName, "M", icon);
        attach(node);
        stack.push(node);
        if (sub != null) {
            sub.accept(this);
        }
        stack.pop();
        last = node; // lambda 结束后 setter 作用于目录本身
        return this;
    }

    /** M 目录 + lambda 子级（无图标）。 */
    public ErpMenus dir(String menuId, String menuName, Consumer<ErpMenus> sub) {
        return dir(menuId, menuName, null, sub);
    }

    /** M 目录（无 lambda：后续声明进入其子级，{@link #up()} 返回上一级）。 */
    public ErpMenus dir(String menuId, String menuName, String icon) {
        ErpMenuVO node = node(menuId, menuName, "M", icon);
        attach(node);
        stack.push(node);
        return this;
    }

    /** M 目录（无图标、无 lambda，配合 {@link #up()}）。 */
    public ErpMenus dir(String menuId, String menuName) {
        return dir(menuId, menuName, (String) null);
    }

    /** M 目录 + 全属性 spec + lambda 子级（最灵活，属性一次填齐）。 */
    public ErpMenus dir(Consumer<MenuSpec> spec, Consumer<ErpMenus> sub) {
        ErpMenuVO node = node(null, null, "M", null);
        if (spec != null) {
            spec.accept(new MenuSpec(node));
        }
        attach(node);
        stack.push(node);
        if (sub != null) {
            sub.accept(this);
        }
        stack.pop();
        last = node; // lambda 结束后 setter 作用于目录本身
        return this;
    }

    /** M 目录 + 全属性 spec（无 lambda，配合 {@link #up()}；保持子级上下文直到 up()）。 */
    public ErpMenus dir(Consumer<MenuSpec> spec) {
        ErpMenuVO node = node(null, null, "M", null);
        if (spec != null) {
            spec.accept(new MenuSpec(node));
        }
        attach(node);
        stack.push(node);
        return this;
    }

    /** C 菜单（叶子页面）。 */
    public ErpMenus menu(String menuId, String menuName, String icon) {
        attach(node(menuId, menuName, "C", icon));
        return this;
    }

    /**
     * C 菜单（叶子页面）+ 自动按钮：扫描指定 controller 的端点方法，
     * 按 {@link ControllerPermScanner} 规则生成 F 按钮挂到该菜单下。
     *
     * @param controllers controller 类（可多个；null 元素忽略）
     */
    public ErpMenus menu(String menuId, String menuName, String icon, Class<?>... controllers) {
        ErpMenuVO node = node(menuId, menuName, "C", icon);
        attach(node);
        attachPermsFrom(node, controllers);
        return this;
    }

    /** C 菜单（无图标）。 */
    public ErpMenus menu(String menuId, String menuName) {
        return menu(menuId, menuName, (String) null);
    }

    /** C 菜单 + 全属性 spec（所有表字段一次填齐）。 */
    public ErpMenus menu(Consumer<MenuSpec> spec) {
        ErpMenuVO node = node(null, null, "C", null);
        if (spec != null) {
            spec.accept(new MenuSpec(node));
        }
        attach(node);
        return this;
    }

    /** F 按钮权限（单参数：perms 同时作为 menuId/menuName/perms）。 */
    public ErpMenus perm(String perms) {
        return perm(perms, perms, perms);
    }

    /** F 按钮权限（显式 menuId/menuName/perms）。 */
    public ErpMenus perm(String menuId, String menuName, String perms) {
        ErpMenuVO node = node(menuId, menuName, "F", null);
        node.setPerms(perms);
        attach(node);
        return this;
    }

    /** F 按钮权限 + 全属性 spec（所有表字段一次填齐）。 */
    public ErpMenus perm(Consumer<MenuSpec> spec) {
        ErpMenuVO node = node(null, null, "F", null);
        if (spec != null) {
            spec.accept(new MenuSpec(node));
        }
        attach(node);
        return this;
    }

    /** 路由条目（path→component 便捷 C 菜单，menuName/path 取 path）。 */
    public ErpMenus route(String path, String url) {
        return route(path, url, null);
    }

    /** 路由条目（带权限标识；url 为页面地址，存入表字段 component）。 */
    public ErpMenus route(String path, String url, String perms) {
        ErpMenuVO node = node(path, path, "C", null);
        node.setPath(path);
        node.setComponent(url);
        node.setPerms(perms);
        attach(node);
        return this;
    }

    /** 返回上一级（配合无 lambda 的 {@link #dir}）。 */
    public ErpMenus up() {
        if (!stack.isEmpty()) {
            stack.pop();
        }
        return this;
    }

    // ===== 链式可选属性（作用于最近声明的节点，均用表字段名）=====

    /** 图标（覆盖位置参数传入的 icon）。 */
    public ErpMenus icon(String icon) {
        if (last != null) {
            last.setIcon(icon);
        }
        return this;
    }

    /** 权限标识。 */
    public ErpMenus perms(String perms) {
        if (last != null) {
            last.setPerms(perms);
        }
        return this;
    }

    /** 显示顺序（小在前，表字段 orderNum）。 */
    public ErpMenus orderNum(int orderNum) {
        if (last != null) {
            last.setOrderNum(orderNum);
        }
        return this;
    }

    /** 组件路径/页面地址（type=C 时作为 iframe 目标，表字段 component）。 */
    public ErpMenus component(String component) {
        if (last != null) {
            last.setComponent(component);
        }
        return this;
    }

    /** 路由路径（相对父级，默认按 menuId）。 */
    public ErpMenus path(String path) {
        if (last != null) {
            last.setPath(path);
        }
        return this;
    }

    /** 是否可见（默认 true；存储为表字段 '0' 显示 / '1' 隐藏）。 */
    public ErpMenus visible(boolean visible) {
        if (last != null) {
            last.setVisible(visible ? "0" : "1");
        }
        return this;
    }

    /**
     * 链式自动按钮：把指定 controller 的端点方法按 {@link ControllerPermScanner} 规则
     * 生成 F 按钮，挂到<b>最近声明</b>的节点（menu/dir/perm）下。
     *
     * @param controllers controller 类（可多个；null 元素忽略）
     */
    public ErpMenus permsFrom(Class<?>... controllers) {
        if (last != null) {
            attachPermsFrom(last, controllers);
        }
        return this;
    }

    /** 构建菜单树（根节点列表快照）。 */
    public List<ErpMenuVO> build() {
        return new ArrayList<>(roots);
    }

    // ===== 全属性 spec =====

    /**
     * 节点全属性填充器（配合 {@code dir(Consumer&lt;MenuSpec&gt;)} / {@code menu(...)} / {@code perm(...)}）。
     * spec 内所有表字段可一次填齐；menuType 由方法名决定，不可填写。
     */
    public static final class MenuSpec {

        /** 被填充的目标节点（spec 所有 setter 最终写入该节点） */
        private final ErpMenuVO node;

        /** 私有构造器：由 {@code dir/menu/perm(Consumer&lt;MenuSpec&gt;)} 传入待填充节点。 */
        private MenuSpec(ErpMenuVO node) {
            this.node = node;
        }

        /** 菜单 ID（表字段，同层级内建议唯一）。 */
        public MenuSpec menuId(String menuId) {
            node.setMenuId(menuId);
            return this;
        }

        /** 菜单名称（表字段）。 */
        public MenuSpec menuName(String menuName) {
            node.setMenuName(menuName);
            return this;
        }

        /** 父菜单 ID（表字段，顶层 '0'）。 */
        public MenuSpec parentId(String parentId) {
            node.setParentId(parentId);
            return this;
        }

        /** 显示顺序（表字段，小在前）。 */
        public MenuSpec orderNum(int orderNum) {
            node.setOrderNum(orderNum);
            return this;
        }

        /** 路由地址（表字段，相对父级，默认按 menuId）。 */
        public MenuSpec path(String path) {
            node.setPath(path);
            return this;
        }

        /** 组件路径/页面地址（表字段；M 目录为 Layout/ParentView，C 菜单为页面组件/iframe 目标）。 */
        public MenuSpec component(String component) {
            node.setComponent(component);
            return this;
        }

        /** 路由参数（表字段，非空时拼接到路由 path 后）。 */
        public MenuSpec query(String query) {
            node.setQuery(query);
            return this;
        }

        /** 路由名称（表字段，空则后端按 path 首字母大写生成）。 */
        public MenuSpec routeName(String routeName) {
            node.setRouteName(routeName);
            return this;
        }

        /** 是否外链（表字段：'0' 是 / '1' 否）。 */
        public MenuSpec isFrame(String isFrame) {
            node.setIsFrame(isFrame);
            return this;
        }

        /** 是否缓存（表字段：'0' 缓存 / '1' 不缓存）。 */
        public MenuSpec isCache(String isCache) {
            node.setIsCache(isCache);
            return this;
        }

        /** 菜单类型（表字段 M/C/F；由方法名定类型，此项仅按需显式覆盖）。 */
        public MenuSpec menuType(String menuType) {
            node.setMenuType(menuType);
            return this;
        }

        /** 显示状态（表字段：'0' 显示 / '1' 隐藏）。 */
        public MenuSpec visible(String visible) {
            node.setVisible(visible);
            return this;
        }

        /** 显示状态便捷填法（boolean → '0'/'1'）。 */
        public MenuSpec visible(boolean visible) {
            node.setVisible(visible ? "0" : "1");
            return this;
        }

        /** 菜单状态（表字段：'0' 正常 / '1' 停用）。 */
        public MenuSpec status(String status) {
            node.setStatus(status);
            return this;
        }

        /** 权限标识。 */
        public MenuSpec perms(String perms) {
            node.setPerms(perms);
            return this;
        }

        /** 菜单图标（emoji / 图片 URL / SVG）。 */
        public MenuSpec icon(String icon) {
            node.setIcon(icon);
            return this;
        }

        /** 是否内置初始化数据（表字段：Y 内置 / N 自定义）。 */
        public MenuSpec builtin(String builtin) {
            node.setBuiltin(builtin);
            return this;
        }
    }

    // ===== 内部 =====

    /**
     * 新建节点并初始化核心表字段。
     *
     * @param menuId   菜单 ID（表字段 menu_id）
     * @param menuName 菜单名称（表字段 menu_name）
     * @param menuType 菜单类型（表字段 menu_type：M 目录 / C 菜单 / F 按钮）
     * @param icon     图标（表字段 icon，可空）
     * @return 已初始化的节点 VO
     */
    private ErpMenuVO node(String menuId, String menuName, String menuType, String icon) {
        ErpMenuVO node = new ErpMenuVO();
        node.setMenuId(menuId);
        node.setMenuName(menuName);
        node.setMenuType(menuType);
        node.setIcon(icon);
        return node;
    }

    /**
     * 将节点挂入当前层级：栈非空 → 作为栈顶（当前父节点）的子节点；栈空 → 加入根节点列表。
     * 同时更新 {@link #last}，使后续链式 setter 作用于该节点。
     *
     * @param node 待挂载节点
     */
    private void attach(ErpMenuVO node) {
        if (!stack.isEmpty()) {
            stack.peek().getChildren().add(node);
        } else {
            roots.add(node);
        }
        last = node;
    }

    /**
     * 将指定 controller 的端点按钮扫描结果挂到父节点下（按声明顺序拼接，排序号连续）。
     *
     * @param parent      挂载目标（菜单/目录节点）
     * @param controllers controller 类数组（null 元素忽略）
     */
    private void attachPermsFrom(ErpMenuVO parent, Class<?>[] controllers) {
        if (controllers == null) {
            return;
        }
        int order = 0;
        for (Class<?> c : controllers) {
            if (c == null) {
                continue;
            }
            List<ErpMenuVO> buttons = ControllerPermScanner.scan(c, order);
            parent.getChildren().addAll(buttons);
            order += buttons.size();
        }
    }
}
