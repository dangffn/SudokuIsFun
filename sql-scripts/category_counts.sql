.mode "line";
SELECT
(SELECT count(*) FROM puzzles WHERE puzzle_category="Easy") AS "Easy Puzzles",
(SELECT count(*) FROM puzzles WHERE puzzle_category="Medium") AS "Medium Puzzles",
(SELECT count(*) FROM puzzles WHERE puzzle_category="Hard") AS "Hard Puzzles",
(SELECT count(*) FROM puzzles) AS "Total";
