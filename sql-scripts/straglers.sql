.mode "line";
SELECT _id, puzzle_category
FROM puzzles
WHERE puzzle_category != "Easy"
AND puzzle_category != "Medium"
AND puzzle_category != "Hard"
AND puzzle_category != "My Puzzles";
