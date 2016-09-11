select _id, estimated_difficulty from puzzles where puzzle_data in
(select puzzle_data from puzzles group by puzzle_data having count(*) > 1);
