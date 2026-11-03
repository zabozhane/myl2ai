import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/**
 * Live Zenthen (Dark Elf Village, lv 20) first plans the Lesser Dark Horror area that killed it on the main
 * server. A controlled death report by a real spawned Lesser Dark Horror (the actor does not die) must make the
 * next ordinary plan choose prey without that monster nearby. No GM, gifts or warp of the actor.
 */
public class DeadlySpotVerify extends OfflineTownVerify {
 static Set<Long> keysWith(Object spots,Location at,int npc,int radius)throws Exception{Set<Long> keys=new HashSet<>();for(Object s:(List<?>)invoke(spots,"getSpots")){Location l=(Location)invoke(s,"getLocation");if(((List<?>)invoke(s,"npcIds")).contains(npc)&&gap(l,at)<=radius)keys.add((Long)invoke(s,"getKey"));}return keys;}
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"Zenthen restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  Object spots=singleton("population.HuntingSpots");Map<?,?> travels=(Map<?,?>)member(m,"traveling").get(m);
  invoke(m,"goHunting",p);Object first=travels.get(p.getObjectId());check(first!=null,"first ordinary plan exists");
  Location death=new Location(-9960,37192,-3704);Set<Long> deadly=keysWith(spots,death,20025,6000);
  Long firstKey=(Long)invoke(first,"getSpotKey");System.out.println("DEADLY INFO first="+firstKey+" target="+invoke(first,"getTargetX")+","+invoke(first,"getTargetY")+" deadlyKeys="+deadly.size());
  check(deadly.contains(firstKey),"the first plan goes to the Lesser Dark Horror area of the live death");
  invoke(m,"clearTravel",p.getObjectId(),null);p.stopMove(null);
  net.sf.l2j.gameserver.model.spawn.L2Spawn spawn=new net.sf.l2j.gameserver.model.spawn.L2Spawn(NpcData.getInstance().getTemplate(20025));
  spawn.setLoc(death.getX(),death.getY(),GeoEngine.getInstance().getHeight(death.getX(),death.getY(),death.getZ()),0);spawn.setRespawnState(false);Monster mob=(Monster)spawn.doSpawn(false);
  // Controlled report at the recorded place of death; the actor stays where it is and alive.
  Location home=location(p);warp(p,death);invoke(m,"reportDeath",p,mob);warp(p,home);mob.deleteMe();
  ((Map<Integer,Long>)member(m,"retryAfter").get(m)).remove(p.getObjectId());((Map<?,?>)member(m,"huntJourneys").get(m)).remove(p.getObjectId());
  invoke(m,"goHunting",p);Object second=travels.get(p.getObjectId());check(second!=null,"after the death an ordinary plan still exists");
  Long secondKey=(Long)invoke(second,"getSpotKey");Location target=new Location((Integer)invoke(second,"getTargetX"),(Integer)invoke(second,"getTargetY"),(Integer)invoke(second,"getTargetZ"));
  System.out.println("DEADLY INFO second="+secondKey+" target="+target+" fromDeath="+(int)gap(target,death));
  check(!deadly.contains(secondKey),"the next plan avoids spots of the monster that killed Zenthen");
  check(!p.isDead()&&!p.isGM()&&!p.isInvul(),"actor alive, no GM or invulnerability");
  System.out.println("DEADLY SPOT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
