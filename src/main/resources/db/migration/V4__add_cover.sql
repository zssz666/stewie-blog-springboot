-- ============================================================
-- V4__add_cover.sql
-- 文章封面图 URL 字段（阶段 7 管理后台：每篇文章可上传一张封面）
-- 在 V3(seed_comments) 之后执行；V1/V2 为 baseline 已跳过
-- ============================================================

ALTER TABLE `t_post`
    ADD COLUMN `cover` VARCHAR(512) NULL DEFAULT NULL COMMENT '封面图 URL'
    AFTER `cover_color`;
