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
