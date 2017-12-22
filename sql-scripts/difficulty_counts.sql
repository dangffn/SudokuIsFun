.mode "line";
SELECT
(SELECT count(*) FROM puzzles WHERE estimated_difficulty<=2) AS "Easy Puzzles (1 to 2)",
(SELECT count(*) FROM puzzles WHERE estimated_difficulty>2 AND estimated_difficulty<=4) AS "Medium Puzzles (3 to 4)",
(SELECT count(*) FROM puzzles WHERE estimated_difficulty>4) AS "Hard Puzzles (5+)",
(SELECT count(*) FROM puzzles) AS "Total";
