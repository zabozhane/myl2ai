import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Live: one Lesser Dark Horror killed Zenthen on the marsh, then it walked to the same monster elsewhere. */
public class SoloKillerVerify extends DeadlySpotVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"Zenthen restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  Object spots=singleton("population.HuntingSpots");Map<?,?> travels=(Map<?,?>)member(m,"traveling").get(m);
  Location death=new Location(-27528,53496,-3672);Set<Long> all=keysWith(spots,death,20025,Integer.MAX_VALUE);
  net.sf.l2j.gameserver.model.spawn.L2Spawn spawn=new net.sf.l2j.gameserver.model.spawn.L2Spawn(NpcData.getInstance().getTemplate(20025));
  spawn.setLoc(death.getX(),death.getY(),GeoEngine.getInstance().getHeight(death.getX(),death.getY(),death.getZ()),0);spawn.setRespawnState(false);Monster mob=(Monster)spawn.doSpawn(false);
  Location home=location(p);warp(p,death);invoke(m,"reportDeath",p,mob);warp(p,home);mob.deleteMe();
  invoke(m,"goHunting",p);Object plan=travels.get(p.getObjectId());check(plan!=null,"an ordinary plan still exists");
  Long key=(Long)invoke(plan,"getSpotKey");System.out.println("SOLO KILLER INFO ldhSpots="+all.size()+" plan="+key+" target="+invoke(plan,"getTargetX")+","+invoke(plan,"getTargetY"));
  check(!all.contains(key),"after a one-on-one death the monster's spots are avoided everywhere");
  System.out.println("SOLO KILLER ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
