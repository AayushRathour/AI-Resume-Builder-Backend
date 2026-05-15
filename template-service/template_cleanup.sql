-- Template cleanup migration (MySQL 8+)
-- 1) Backup templates table before running.
-- 2) Run updates to remove duplicate link-label artifacts and script tags.

CREATE TABLE IF NOT EXISTS templates_backup_20260512 AS
SELECT * FROM templates;

UPDATE templates
SET html_content = REGEXP_REPLACE(
    html_content,
    '<script[^>]*>[\\s\\S]*?</script>',
    ''
)
WHERE html_content REGEXP '<script';

UPDATE templates
SET html_content = REGEXP_REPLACE(
    html_content,
    '</a>\\s*"\\s*>\\s*(LinkedIn|GitHub|Portfolio|Website|Behance|LeetCode|CodeChef|Codeforces)',
    '</a>',
    1,
    0,
    'i'
)
WHERE html_content REGEXP '</a>\\s*"\\s*>\\s*(LinkedIn|GitHub|Portfolio|Website|Behance|LeetCode|CodeChef|Codeforces)';

UPDATE templates
SET html_content = REGEXP_REPLACE(
    html_content,
    '(\\{\\{\\s*[a-zA-Z_]+\\s*\\}\\})\\s*\\1',
    '\\1'
)
WHERE html_content REGEXP '(\\{\\{\\s*[a-zA-Z_]+\\s*\\}\\})\\s*\\1';
