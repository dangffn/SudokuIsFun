.mode "line";
UPDATE puzzles SET saved=NULL, save_duration=0, save_data=null, save_icon=null, save_x=null, save_y=null, save_r=null, save_finished=null, save_assisted=null;
DELETE FROM stats;
INSERT INTO stats (_id) SELECT _id FROM puzzles;
