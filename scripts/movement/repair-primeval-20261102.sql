-- One-time position repair for the three verified stranded Dark Mystic bots.
-- Stop the game and take a database backup before applying manually.
-- Not part of the automatic schema/bootstrap or bot AI. Updates no player characters.
-- Targets are actual class 38 starter spawns from data/xml/classes/darkMystic.xml.
START TRANSACTION;
SET @repair_rows=0;

UPDATE autobots SET x=28295,y=11063,z=-4224
WHERE obj_Id=268487232 AND char_name='Weapons' AND classid=38 AND level=4
  AND x=10468 AND y=-24569 AND z BETWEEN -3700 AND -3500;
SET @repair_rows=@repair_rows+ROW_COUNT();

UPDATE autobots SET x=28302,y=11008,z=-4224
WHERE obj_Id=268484168 AND char_name='BALiCTiK' AND classid=38 AND level=5
  AND x=11729 AND y=-24224 AND z BETWEEN -3700 AND -3500;
SET @repair_rows=@repair_rows+ROW_COUNT();

UPDATE autobots SET x=28377,y=10916,z=-4224
WHERE obj_Id=268485396 AND char_name='Verzilas' AND classid=38 AND level=5
  AND x=10543 AND y=-24632 AND z BETWEEN -3700 AND -3500;
SET @repair_rows=@repair_rows+ROW_COUNT();

SELECT @repair_rows AS repaired_positions;
SELECT obj_Id,char_name,level,classid,x,y,z,exp FROM autobots
WHERE obj_Id IN (268487232,268484168,268485396) ORDER BY char_name;
COMMIT;
