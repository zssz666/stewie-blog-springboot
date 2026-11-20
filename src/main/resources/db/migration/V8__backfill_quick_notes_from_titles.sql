UPDATE t_post
SET post_type = 1, updated_at = updated_at
WHERE post_type = 2
  AND (title LIKE '%技术练习%' OR title LIKE '%今日八股%');
