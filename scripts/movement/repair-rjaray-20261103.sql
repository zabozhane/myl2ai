-- One-time position repair for the verified stranded Dwarven Fighter RjaRay (lv 5) in Schuttgart:
-- 21 adena, no Scroll of Escape, no checked walk to suitable prey, Dwarven Village ride costs 4400.
-- Stop the game and take a database backup before applying manually.
-- Not part of the automatic schema/bootstrap or bot AI. Updates no player characters.
-- Target is an actual class 53 starter spawn from data/xml/classes/dwarfFighter.xml.
START TRANSACTION;
UPDATE autobots SET x=108644,y=-173947,z=-400
WHERE obj_Id=268484688 AND char_name='RjaRay' AND classid=53 AND level=5
  AND x=87048 AND y=-143448 AND z=-1288;
SELECT ROW_COUNT() AS repaired_positions;
SELECT obj_Id,char_name,level,classid,x,y,z,exp FROM autobots WHERE obj_Id=268484688;
COMMIT;
