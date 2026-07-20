-- ============================================================
-- V5__search_index.sql
-- 扩展全文索引，将 excerpt 纳入检索范围（ngram 中文分词）
-- ============================================================

-- 先删掉阶段 2 建立的仅含 title/content 的索引
ALTER TABLE t_post DROP INDEX ft_post_title_content;

-- 重建为 title + excerpt + content 的 ngram 全文索引
ALTER TABLE t_post ADD FULLTEXT KEY ft_post_search (title, excerpt, content) WITH PARSER ngram;
