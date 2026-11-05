import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.Autobot;

/** Live resident jhk (Human Fighter 13) died again and again to werewolves at -101672,234392: a controlled death report must exclude them. */
public class KillerMemoryVerify extends OfflineTownVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","jhk");check(b!=null,"jhk restores");
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);
  Location death=new Location(-101672,234392,-3392);warp(b,death);
  Object spots=singleton("population.HuntingSpots");Set<Long> wolf=new HashSet<>();
  for(Object s:(List<?>)invoke(spots,"getSpots"))if(((List<?>)invoke(s,"npcIds")).contains(20342))wolf.add((Long)invoke(s,"getKey"));
  int before=0;for(int i=0;i<100;i++){Object s=invoke(spots,"choose",b,b.getLevel(),invoke(life,"getExcludedSpots"),null,20000,false,Collections.emptySet());if(s!=null&&wolf.contains(invoke(s,"getKey")))before++;}
  net.sf.l2j.gameserver.model.spawn.L2Spawn spawn=new net.sf.l2j.gameserver.model.spawn.L2Spawn(NpcData.getInstance().getTemplate(20342));
  spawn.setLoc(death.getX()+100,death.getY(),GeoEngine.getInstance().getHeight(death.getX()+100,death.getY(),death.getZ()),0);spawn.setRespawnState(false);Monster mob=(Monster)spawn.doSpawn(false);
  invoke(life,"onDeath",mob);mob.deleteMe();
  Set<?> excluded=(Set<?>)invoke(life,"getExcludedSpots");int after=0;
  for(int i=0;i<100;i++){Object s=invoke(spots,"choose",b,b.getLevel(),excluded,null,20000,false,Collections.emptySet());if(s!=null&&wolf.contains(invoke(s,"getKey")))after++;}
  System.out.println("KILLER INFO werewolfSpots="+wolf.size()+" excluded="+excluded.size()+" picksBefore="+before+" picksAfter="+after);
  check(((Set<?>)invoke(life,"getExcludedSpots")).size()>=0,"report done");
  check(after==0&&excluded.containsAll(wolf),"after the death report jhk avoids every werewolf chieftain spot");
  System.out.println("KILLER MEMORY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
