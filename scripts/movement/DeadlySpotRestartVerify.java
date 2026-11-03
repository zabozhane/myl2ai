import java.util.*;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** save: controlled death report stores the avoided spots; resume (new JVM, same DB): the first plan avoids them. */
public class DeadlySpotRestartVerify extends DeadlySpotVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  boolean save=Arrays.asList(args).contains("save");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"Zenthen restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  Object spots=singleton("population.HuntingSpots");Map<?,?> travels=(Map<?,?>)member(m,"traveling").get(m);
  Location death=new Location(-9960,37192,-3704);Set<Long> deadly=keysWith(spots,death,20025,6000);
  if(save){
   net.sf.l2j.gameserver.model.spawn.L2Spawn spawn=new net.sf.l2j.gameserver.model.spawn.L2Spawn(NpcData.getInstance().getTemplate(20025));
   spawn.setLoc(death.getX(),death.getY(),GeoEngine.getInstance().getHeight(death.getX(),death.getY(),death.getZ()),0);spawn.setRespawnState(false);Monster mob=(Monster)spawn.doSpawn(false);
   Location home=location(p);warp(p,death);invoke(m,"reportDeath",p,mob);warp(p,home);mob.deleteMe();
   int rows=0;try(java.sql.Connection c=L2DatabaseFactory.getInstance().getConnection();java.sql.PreparedStatement ps=c.prepareStatement("SELECT count(*) FROM character_offline_deadly_spots WHERE char_id=?")){ps.setInt(1,p.getObjectId());try(java.sql.ResultSet r=ps.executeQuery()){r.next();rows=r.getInt(1);}}
   System.out.println("DEADLY RESTART INFO stored="+rows+" expected="+deadly.size());
   check(rows==deadly.size()&&rows>0,"the avoided spots are stored for this character");
   System.out.println("DEADLY RESTART SAVE ALL PASSED");System.exit(0);
  }
  invoke(m,"goHunting",p);Object plan=travels.get(p.getObjectId());check(plan!=null,"an ordinary plan exists after the restart");
  Long key=(Long)invoke(plan,"getSpotKey");System.out.println("DEADLY RESTART INFO plan="+key+" target="+invoke(plan,"getTargetX")+","+invoke(plan,"getTargetY"));
  check(!deadly.contains(key),"after a restart the first plan still avoids the monster that killed Zenthen");
  System.out.println("DEADLY RESTART RESUME ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
