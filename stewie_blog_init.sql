-- ============================================================
-- stewie_blog_init.sql
-- Stewie Blog 完整建库脚本（可直接在腾讯云 CDB 控制台执行）
-- 包含：建库 + 9 张表 + 索引 + 种子数据
-- 默认管理员：admin / admin123
-- 字符集：utf8mb4
-- 生成日期：2026-07-06
-- ============================================================

CREATE DATABASE IF NOT EXISTS `stewie_blog`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `stewie_blog`;

-- ============================================================
-- V1__init_schema.sql
-- Stewie Blog 初始 schema：9 张表
-- 字符集 utf8mb4 / 引擎 InnoDB
-- 关联关系走应用层维护（MyBatis-Plus），不建物理外键
-- ============================================================

-- -----------------------------------------------------------
-- 1. 管理员账户
-- -----------------------------------------------------------
CREATE TABLE `t_user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username`      VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    `password`      VARCHAR(100) NOT NULL COMMENT '密码（BCrypt hash）',
    `nickname`      VARCHAR(50)           DEFAULT NULL COMMENT '昵称',
    `avatar`        VARCHAR(255)          DEFAULT NULL COMMENT '头像 URL',
    `email`         VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1=正常 0=禁用',
    `last_login_at` DATETIME              DEFAULT NULL COMMENT '最后登录时间',
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0=未删 1=已删',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='管理员账户';

-- -----------------------------------------------------------
-- 2. 分类
-- -----------------------------------------------------------
CREATE TABLE `t_category` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(50) NOT NULL COMMENT '分类名',
    `slug`       VARCHAR(50)          DEFAULT NULL COMMENT 'URL 友好标识',
    `sort`       INT         NOT NULL DEFAULT 0 COMMENT '排序（越小越靠前）',
    `deleted`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_category_name` (`name`),
    UNIQUE KEY `uk_category_slug` (`slug`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='文章分类';

-- -----------------------------------------------------------
-- 3. 标签
-- -----------------------------------------------------------
CREATE TABLE `t_tag` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(50) NOT NULL COMMENT '标签名',
    `slug`       VARCHAR(50)          DEFAULT NULL COMMENT 'URL 友好标识',
    `deleted`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tag_name` (`name`),
    UNIQUE KEY `uk_tag_slug` (`slug`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='文章标签';

-- -----------------------------------------------------------
-- 4. 作者信息（关于页，单条）
-- -----------------------------------------------------------
CREATE TABLE `t_author` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`       VARCHAR(50)  NOT NULL COMMENT '姓名',
    `role`       VARCHAR(50)           DEFAULT NULL COMMENT '职位/身份',
    `bio`        TEXT                  DEFAULT NULL COMMENT '简介',
    `avatar`     VARCHAR(255)          DEFAULT NULL COMMENT '头像 URL',
    `skills`     JSON                  DEFAULT NULL COMMENT '技能数组，如 ["Vue 3","TypeScript"]',
    `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='作者信息';

-- -----------------------------------------------------------
-- 5. 文章主体
-- -----------------------------------------------------------
CREATE TABLE `t_post` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `slug`         VARCHAR(100) NOT NULL COMMENT 'URL 友好路径，唯一',
    `title`        VARCHAR(200) NOT NULL COMMENT '标题',
    `excerpt`      TEXT                  DEFAULT NULL COMMENT '摘要',
    `content`      LONGTEXT              DEFAULT NULL COMMENT '正文 HTML',
    `cover_color`  VARCHAR(20)           DEFAULT NULL COMMENT '封面色值',
    `category_id`  BIGINT                DEFAULT NULL COMMENT '分类 ID（逻辑 FK → t_category.id）',
    `publish_date` DATE                  DEFAULT NULL COMMENT '发布日期（前端展示用）',
    `reading_time` INT                   DEFAULT 0 COMMENT '阅读时长（分钟）',
    `views`        BIGINT       NOT NULL DEFAULT 0 COMMENT '浏览量',
    `likes`        BIGINT       NOT NULL DEFAULT 0 COMMENT '点赞数',
    `status`       TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0=草稿 1=已发布',
    `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `published_at` DATETIME              DEFAULT NULL COMMENT '发布时间戳',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_slug` (`slug`),
    KEY `idx_post_category` (`category_id`),
    KEY `idx_post_status` (`status`),
    KEY `idx_post_published_at` (`published_at` DESC),
    -- 中文全文索引（ngram 解析器，双字分词）
    FULLTEXT KEY `ft_post_title_content` (`title`, `content`) WITH PARSER ngram
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='文章主体';

-- -----------------------------------------------------------
-- 6. 文章-标签 多对多关联
-- -----------------------------------------------------------
CREATE TABLE `t_post_tag` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id`    BIGINT   NOT NULL COMMENT '文章 ID（逻辑 FK → t_post.id）',
    `tag_id`     BIGINT   NOT NULL COMMENT '标签 ID（逻辑 FK → t_tag.id）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_tag` (`post_id`, `tag_id`),
    KEY `idx_pt_tag` (`tag_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='文章-标签关联';

-- -----------------------------------------------------------
-- 7. 评论（支持楼中楼）
-- -----------------------------------------------------------
CREATE TABLE `t_comment` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id`    BIGINT      NOT NULL COMMENT '文章 ID（逻辑 FK → t_post.id）',
    `parent_id`  BIGINT               DEFAULT NULL COMMENT '父评论 ID（顶级评论为 NULL）',
    `nickname`   VARCHAR(50) NOT NULL COMMENT '评论者昵称',
    `email`      VARCHAR(100)         DEFAULT NULL COMMENT '邮箱（不公开显示）',
    `content`    TEXT        NOT NULL COMMENT '评论内容',
    `status`     TINYINT     NOT NULL DEFAULT 0 COMMENT '状态：0=待审核 1=已通过 2=垃圾',
    `ip`         VARCHAR(45)          DEFAULT NULL COMMENT '评论者 IP',
    `deleted`    TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_comment_post` (`post_id`),
    KEY `idx_comment_parent` (`parent_id`),
    KEY `idx_comment_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='文章评论';

-- -----------------------------------------------------------
-- 8. 社交链接
-- -----------------------------------------------------------
CREATE TABLE `t_social_link` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `author_id`  BIGINT      NOT NULL COMMENT '作者 ID（逻辑 FK → t_author.id）',
    `label`      VARCHAR(50) NOT NULL COMMENT '标签名（GitHub/Twitter/Email 等）',
    `href`       VARCHAR(255) NOT NULL COMMENT '链接地址',
    `icon`       VARCHAR(50)          DEFAULT NULL COMMENT '图标标识',
    `sort`       INT         NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_social_author` (`author_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='作者社交链接';

-- -----------------------------------------------------------
-- 9. 点赞记录（指纹去重）
-- -----------------------------------------------------------
CREATE TABLE `t_like_record` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `post_id`     BIGINT      NOT NULL COMMENT '文章 ID（逻辑 FK → t_post.id）',
    `fingerprint` VARCHAR(64) NOT NULL COMMENT '用户指纹（IP+UA hash）',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_like_post_fp` (`post_id`, `fingerprint`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='点赞记录（防重复点赞）';

-- ============================================================
-- V2__seed_data.sql
-- 种子数据：管理员 / 分类 / 标签 / 作者 / 社交链接 / 6 篇文章 / 关联
-- 数据来源：前端 src/data/posts.ts
-- 默认管理员：admin / admin123（BCrypt 已加密）
-- ============================================================

-- -----------------------------------------------------------
-- 管理员（密码 admin123，BCrypt hash）
-- -----------------------------------------------------------
INSERT INTO `t_user` (`id`, `username`, `password`, `nickname`, `email`, `status`)
VALUES (1, 'admin', '$2a$10$pvGTR2G8OAJgUtJFJHyUROxy1YHdYw0ggA9FmI.p8K36FAtYIEXjK', 'Stewie', '2133290569@qq.com', 1);

-- -----------------------------------------------------------
-- 分类
-- -----------------------------------------------------------
INSERT INTO `t_category` (`id`, `name`, `slug`, `sort`) VALUES
    (1, '问题解决', 'problem-solving', 1),
    (2, '学习心得', 'learning', 2);

-- -----------------------------------------------------------
-- 标签
-- -----------------------------------------------------------
INSERT INTO `t_tag` (`id`, `name`, `slug`) VALUES
    (1, 'Vue',       'vue'),
    (2, 'TypeScript','typescript'),
    (3, 'Vite',      'vite'),
    (4, 'CSS',       'css'),
    (5, 'Pinia',     'pinia');

-- -----------------------------------------------------------
-- 作者信息
-- -----------------------------------------------------------
INSERT INTO `t_author` (`id`, `name`, `role`, `bio`, `skills`) VALUES
    (1, 'Stewie', '前端工程师',
     '本科毕业在一个小公司做全栈的社畜，弄个博客记录一下自己工作遇到的问题和一些心得。',
     '["Vue 3","TypeScript","Vite","Pinia","Node.js","koa","java"]');

-- -----------------------------------------------------------
-- 社交链接
-- -----------------------------------------------------------
INSERT INTO `t_social_link` (`id`, `author_id`, `label`, `href`, `sort`) VALUES
    (1, 1, 'GitHub',  'https://github.com/zssz666/',   1),
    (2, 1, 'Twitter', 'https://twitter.com',           2),
    (3, 1, 'Email',   'mailto:2133290569@qq.com',      3);

-- -----------------------------------------------------------
-- 文章（6 篇，content 为 HTML，单引号已转义）
-- -----------------------------------------------------------
INSERT INTO `t_post` (`id`, `slug`, `title`, `excerpt`, `content`, `cover_color`, `category_id`, `publish_date`, `reading_time`, `views`, `likes`, `status`, `published_at`, `created_at`, `updated_at`) VALUES

(1, 'vue-watch-infinite-loop', 'Vue 3 watch 死循环？我是这样排查的',
 '一个看着没问题的 watch，页面一加载就疯狂触发。从 DevTools 依赖追踪到最终改写，记录下我排查的完整过程。',
 '<p>线上一个表单页面突然卡死，打开控制台才发现是 watch 触发了无限循环。这种问题在 Composition API 中尤为隐蔽，记录下我的排查思路。</p>
<h2>问题复现</h2>
<p>场景很简单：监听一个 <code>ref</code>，在回调里又修改了它依赖的另一个状态，而这个状态恰好又出现在被监听的源里。于是修改 → 触发 → 再修改，死循环就形成了。</p>
<h2>排查的第一步：定位触发源</h2>
<p>用 Vue DevTools 的「组件依赖」面板，能直观看到某个 ref 被谁读取、又被谁修改。我发现回调里隐式读取了监听源本身——这是循环的根因。</p>
<h2>三种解法</h2>
<ul>
  <li><strong>拆分状态</strong>：把「读」和「写」拆到不同的 ref，从结构上切断回路</li>
  <li><strong>用 watchOnce 或条件守卫</strong>：在回调里加 <code>if</code> 判断，满足条件才执行副作用</li>
  <li><strong>换用 computed</strong>：能用派生值表达的，优先用 computed，它天生无副作用</li>
</ul>
<blockquote>watch 是副作用，副作用最容易制造循环。能派生就别监听。</blockquote>
<p>最终我把那段逻辑改写成了 computed + 一个显式的事件触发，循环彻底消失。这次经历让我对「响应式边界」有了更深的敬畏。</p>',
 '#1e3a5f', 1, '2026-06-22', 6, 1284, 0, 1, '2026-06-22 00:00:00', '2026-06-22 00:00:00', '2026-06-22 00:00:00'),

(2, 'typescript-narrowing-pitfalls', 'TypeScript 类型收窄总是不生效？多半是这几个坑',
 'typeof、in、判别联合……类型收窄看着简单，但在闭包和数组解构里经常失效。总结下我反复踩过的几个典型场景。',
 '<p>刚用 TypeScript 时，我总在「为什么编译器说这个类型不存在」上卡住。后来才明白，问题大多出在类型收窄（narrowing）没到位。</p>
<h2>判别联合是首选</h2>
<p>当一组类型有共同字段时，给它们加一个字面量类型的 <code>kind</code> 标记，TS 就能在 <code>switch</code> 里自动收窄。这比 <code>typeof</code> 和 <code>in</code> 都更可靠、更可读。</p>
<h2>类型谓词（user-defined type guard）</h2>
<p>当内置收窄不够用时，写一个返回 <code>val is Foo</code> 的函数，能把复杂的判断逻辑封装成可复用的收窄规则。这是我后来用得最多的技巧。</p>
<h2>收窄会丢失的地方</h2>
<ul>
  <li>回调函数里：TS 默认不收窄闭包内的变量，因为它们可能被改写</li>
  <li>数组解构后：收窄信息不会传播到解构出的元素</li>
</ul>
<blockquote>类型不是写给人看的约束，而是写给编译器的契约——你越清晰，它越能帮你。</blockquote>
<p>理解了收窄的边界，写 TS 就从「和编译器吵架」变成了「和编译器合作」。</p>',
 '#2d4373', 2, '2026-06-15', 7, 956, 0, 1, '2026-06-15 00:00:00', '2026-06-15 00:00:00', '2026-06-15 00:00:00'),

(3, 'vite-proxy-cors-fix', 'Vite 开发服务器跨域？一套代理配置搞定',
 '本地调试接口频繁遇到 CORS 报错。这篇文章给出一份亲测可用的 Vite proxy 配置，并解释每条规则背后的原理。',
 '<p>前端本地起在 5173 端口，后端在 8080，浏览器直接请求就报 CORS。开发阶段不必在后端配跨域，用 Vite 的 proxy 转发最省事。</p>
<h2>最小可用配置</h2>
<p>在 <code>vite.config.ts</code> 的 <code>server.proxy</code> 里，把以 <code>/api</code> 开头的请求转发到后端地址，并重写路径。关键是要设 <code>changeOrigin: true</code>，让请求头的 host 改成目标地址。</p>
<pre><code>import { defineConfig } from ''vite''
import vue from ''@vitejs/plugin-vue''

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      ''/api'': {
        target: ''http://localhost:8080'',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''''),
      },
    },
  },
})</code></pre>
<h2>容易踩的三个坑</h2>
<ul>
  <li><strong>路径重写写反</strong>：<code>rewrite</code> 的正则要把前缀去掉，别误删业务路径</li>
  <li><strong>WebSocket 代理</strong>：如果是 ws，记得加 <code>ws: true</code></li>
  <li><strong>cookie 跨域</strong>：带凭证时后端 <code>Access-Control-Allow-Credentials</code> 要为 true，且不能用通配符</li>
</ul>
<blockquote>开发环境的跨域，永远应该在前端工具层解决，而不是去改后端。</blockquote>
<p>配好之后，本地代码里直接写 <code>/api/xxx</code>，无感地走代理，生产再用 nginx 处理，开发体验丝滑许多。</p>',
 '#3b5bdb', 1, '2026-06-08', 5, 2103, 0, 1, '2026-06-08 00:00:00', '2026-06-08 00:00:00', '2026-06-08 00:00:00'),

(4, 'css-centering-pitfalls', 'CSS 垂直居中又出问题了？聊聊我踩过的那些坑',
 'flex、grid、absolute+transform……垂直居中的方案换了好几代，但每代都有自己坑。回顾下我实际遇到过的失效场景。',
 '<p>「如何垂直居中一个元素」几乎是每个前端都被问过的问题。但更有意思的是，这个问题的答案随着 CSS 演进不断变化，而每代方案都有自己坑。</p>
<h2>第一代：表格布局</h2>
<p>早年用 <code>display: table-cell</code> + <code>vertical-align</code>，把元素当表格单元格来居中。能用，但语义混乱、灵活性差。</p>
<h2>第二代：绝对定位 + 负 margin</h2>
<p>知道元素宽高时，用 <code>position: absolute</code> 配合负 margin 居中。局限是要先知道尺寸，响应式下捉襟见肘。</p>
<h2>第三代：transform</h2>
<p><code>top: 50%; transform: translateY(-50%)</code> 解决了未知尺寸的居中，但脱离了文档流，对父容器有要求。</p>
<h2>第四代：Flexbox</h2>
<p><code>display: flex; align-items: center; justify-content: center;</code> 一行搞定，语义清晰、不脱离文档流。这是现在 90% 场景的答案。</p>
<h2>第五代：Grid</h2>
<p>Grid 的 <code>place-items: center</code> 更进一步，二维场景下更强大。布局工具的进化，本质是让开发者更少地「对抗」浏览器。</p>
<blockquote>每一代方案的消失，都是浏览器替我们承担了更多复杂性。</blockquote>
<p>理解这段演进，就不只是记住一个 API，而是理解 CSS 设计哲学的方向。</p>',
 '#4a6b8a', 2, '2026-05-30', 6, 743, 0, 1, '2026-05-30 00:00:00', '2026-05-30 00:00:00', '2026-05-30 00:00:00'),

(5, 'pinia-persist-pitfall', 'Pinia 持久化丢数据？多半是序列化的坑',
 '给 Pinia store 接了持久化插件，刷新后方法全没了、页面直接报错。追根溯源是 JSON 序列化丢掉了响应式和函数。',
 '<p>给购物车 store 接上持久化后，刷新页面发现 cart 里的商品对象还在，但一些计算用的方法不见了，页面直接报错。这是一次典型的序列化陷阱。</p>
<h2>问题根因</h2>
<p><code>JSON.stringify</code> 会丢掉函数、Symbol、以及 <code>Proxy</code> 包装下的响应式元信息。存进去的是一个纯数据快照，读回来就成了普通对象，原本挂在 store 上的方法自然没了。</p>
<h2>解法：只持久化必要的字段</h2>
<p>不要把整个 store 状态无脑序列化。在持久化插件的 <code>serializer</code> 里，只挑出真正的业务数据（如商品列表），方法与计算属性留在 store 定义里，每次从 store 实例取即可。</p>
<h2>进阶：版本迁移</h2>
<p>持久化的数据结构会随业务演进。给存档加一个 <code>version</code> 字段，读取时做迁移，能避免老数据结构导致的运行时错误。</p>
<blockquote>持久化的不是「状态」，而是「可重建状态的最小数据」。</blockquote>
<p>想清楚「哪些是数据、哪些是行为」之后，持久化的边界就清晰了，问题也迎刃而解。</p>',
 '#5a7299', 1, '2026-05-20', 5, 612, 0, 1, '2026-05-20 00:00:00', '2026-05-20 00:00:00', '2026-05-20 00:00:00'),

(6, 'options-to-composition-refactor', '800行组件从 Options 迁到 script setup，我踩了这些坑',
 '真正动手迁移一个 800 行的表单组件后，才发现 Composition API 的好处不在写法本身，而在逻辑边界变清晰了。',
 '<p>一直听说 Composition API 更好，但没真正迁移过大型组件。这次把一个 800 行的表单组件从 Options API 迁到 <code>script setup</code>，收获比预期大得多。</p>
<h2>最大的改变：逻辑按关注点聚合</h2>
<p>迁移前，表单校验逻辑被拆散在 data、methods、computed、watch 里，改一个字段要跳四个地方。迁移后，我把校验抽成一个 <code>useValidation</code> 组合式函数，相关的 state、计算、监听全在一起，改起来不用再满文件跳。</p>
<h2>逻辑复用变得显式</h2>
<p>以前用 mixin，不知道哪个方法从哪混进来的；现在用 composables，输入输出一目了然，类型也能跟着走。这种「显式」让代码可读性质的飞跃。</p>
<h2>可测试性的提升</h2>
<ul>
  <li>组合式函数是纯函数式的，可以直接在组件外单测</li>
  <li>不再依赖 <code>this</code>，mock 依赖更容易</li>
</ul>
<blockquote>重构的价值不在「换写法」，而在让逻辑的边界变得清晰。</blockquote>
<p>迁移完成后，组件行数没少多少，但「理解一个功能要读多少代码」从一大段变成了一个函数。这就是 Composition API 的真正红利。</p>',
 '#1e3a5f', 2, '2026-05-10', 7, 1587, 0, 1, '2026-05-10 00:00:00', '2026-05-10 00:00:00', '2026-05-10 00:00:00');

-- -----------------------------------------------------------
-- 文章-标签关联（每篇文章对应一个标签）
-- -----------------------------------------------------------
INSERT INTO `t_post_tag` (`post_id`, `tag_id`) VALUES
    (1, 1),  -- vue-watch-infinite-loop → Vue
    (2, 2),  -- typescript-narrowing-pitfalls → TypeScript
    (3, 3),  -- vite-proxy-cors-fix → Vite
    (4, 4),  -- css-centering-pitfalls → CSS
    (5, 5),  -- pinia-persist-pitfall → Pinia
    (6, 1);  -- options-to-composition-refactor → Vue
