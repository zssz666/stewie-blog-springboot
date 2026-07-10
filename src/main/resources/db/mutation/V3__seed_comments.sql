-- ============================================================
-- V3__seed_comments.sql
-- 阶段 5 互动模块：为演示评论区写入若干「已通过」评论（含楼中楼）
-- 依赖 t_comment（已在 stewie_blog_init.sql 建好，Flyway baseline=2 跳过 V1/V2）
-- Flyway 仅执行一次，重复部署安全。
-- ============================================================

INSERT INTO `t_comment` (`id`, `post_id`, `parent_id`, `nickname`, `email`, `content`, `status`, `ip`, `created_at`)
VALUES
    -- 文章 1（vue-watch-infinite-loop）的评论 + 作者回复
    (1, 1, NULL, '林夕', 'linxi@example.com', '看完受益良多，watch 死循环确实坑过我，computed 改写的思路很清晰。', 1, '127.0.0.1', '2026-06-23 09:12:00'),
    (2, 1, 1,    'Stewie', '2133290569@qq.com', '谢谢支持～后面会写更多响应式踩坑记录。', 1, '127.0.0.1', '2026-06-23 10:05:00'),
    (3, 1, NULL, '路过的开发者', NULL, '判别联合那个例子很实用，已经收藏。', 1, '127.0.0.1', '2026-06-24 14:30:00'),
    -- 文章 2（typescript-narrowing-pitfalls）的评论
    (4, 2, NULL, 'TS 小白', 'tsrookie@example.com', '类型谓词终于搞懂了，之前一直和编译器吵架 😂', 1, '127.0.0.1', '2026-06-16 20:11:00');
