ALTER TABLE t_post
    ADD COLUMN post_type TINYINT UNSIGNED NOT NULL DEFAULT 2 COMMENT '1=随手记，2=技术文章',
    ADD CONSTRAINT chk_t_post_type CHECK (post_type IN (1, 2));

UPDATE t_post AS post
    JOIN t_post_tag AS post_tag ON post_tag.post_id = post.id
    JOIN t_tag AS tag ON tag.id = post_tag.tag_id
SET post.post_type = 1, post.updated_at = post.updated_at
WHERE tag.name = '随手记';
