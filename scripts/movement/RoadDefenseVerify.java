import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import dev.l2j.autobots.autofarm.AutofarmManager;

/**
 * Live (2026-11-06): Zenthen walked from Kaboo to a lv 15-23 spot, a Kaboo grunt jumped him on the road, and after
 * the fight the roadside was judged an outgrown anchor; the walk restarted every ~70 s and spots were excluded.
 * After a road fight the walk must resume to the same spot, with that spot not marked empty.
 */
public class RoadDefenseVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);
  int id=p.getObjectId();Map<Integer,Object> travels=(Map<Integer,Object>)member(m,"traveling").get(m);
  invoke(m,"goHunting",p);Object trip=travels.get(id);check(trip!=null,"Zenthen starts a walk to a hunting spot");
  Long key=(Long)member(trip,"spotKey").get(trip);int tx=(Integer)member(trip,"targetX").get(trip),ty=(Integer)member(trip,"targetY").get(trip);
  // Pessimistic: the spot being walked to is also the remembered one, so a wrong "outgrown" verdict would exclude it.
  ((Map<Integer,Long>)member(m,"huntedSpot").get(m)).put(id,key);
  System.out.println("ROAD INFO at="+location(p)+" spotKey="+key+" target="+tx+","+ty);
  for(int i=0;i<10;i++){invoke(m,"tick");Thread.sleep(300);}
  // A grunt (lv 7-12) from the Kaboo camp jumps him on the road.
  net.sf.l2j.gameserver.model.spawn.L2Spawn near=SpawnTable.getInstance().getSpawns().stream().filter(s->s.getTemplate().isType("Monster")&&s.getTemplate().getLevel()<=12)
    .min(Comparator.comparingDouble(s->Math.hypot(s.getLocX()-p.getX(),s.getLocY()-p.getY()))).get();
  net.sf.l2j.gameserver.model.spawn.L2Spawn sp=new net.sf.l2j.gameserver.model.spawn.L2Spawn(near.getTemplate());
  sp.setLoc(p.getX()+120,p.getY(),GeoEngine.getInstance().getHeight(p.getX()+120,p.getY(),p.getZ()),0);sp.setRespawnState(false);
  Monster mob=(Monster)sp.doSpawn(false);mob.setTarget(p);mob.addDamageHate(p,0,100);mob.getAI().setIntention(net.sf.l2j.gameserver.enums.IntentionType.ATTACK,p);
  System.out.println("ROAD INFO attacker="+mob.getName()+" lv"+mob.getLevel());
  Map<Integer,Object> road;try{road=(Map<Integer,Object>)member(m,"roadDefense").get(m);}catch(Exception e){road=new HashMap<>();System.out.println("ROAD INFO no roadDefense (old build)");}Map<Integer,Map<Long,Long>> empty=(Map<Integer,Map<Long,Long>>)member(m,"emptySpots").get(m);
  long began=System.currentTimeMillis();boolean defended=false;Object resumed=null;
  while(System.currentTimeMillis()-began<180000){invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
   if(road.containsKey(id)||AutofarmManager.INSTANCE.isAutoFarming(p))defended=true;
   Object t=travels.get(id);if(defended&&t!=null&&!AutofarmManager.INSTANCE.isAutoFarming(p)){resumed=t;break;}Thread.sleep(300);}
  boolean excluded=empty.getOrDefault(id,Collections.emptyMap()).containsKey(key);
  Long rkey=resumed==null?null:(Long)member(resumed,"spotKey").get(resumed);
  System.out.println("ROAD RESULT seconds="+(System.currentTimeMillis()-began)/1000+" defended="+defended+" mobDead="+mob.isDead()+" resumedKey="+rkey+
    " trip="+(resumed==null?null:member(resumed,"trip").get(resumed))+" excluded="+excluded+" at="+location(p));
  check(defended,"Zenthen fights the attacker on the road");
  check(resumed!=null&&key.equals(rkey),"after the fight the walk resumes to the same spot");
  check(!excluded,"the spot the walk was heading for is not marked empty");
  System.out.println("ROAD DEFENSE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
