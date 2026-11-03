import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.player.Experience;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Inject a known failed quest decision; verify durable pause without waiting three hours or replaying a road. */
public class ProfessionRetryMemoryVerify extends WeaponGradeSuppliesVerify {
 static Object runner(Player p)throws Exception{return Class.forName("dev.l2j.autobots.population.quests.QuestRunner").getConstructor(Player.class).newInstance(p);}
 static Object fail(Player p)throws Exception{Object r=runner(p);check((Boolean)invoke(r,"professionBusiness"),"eligible profession starts before the controlled failure");Object path=invoke(r,"currentPath");check(path!=null,"supported real profession path exists");invoke(r,"giveUp",path,"controlled known unreachable road");check(!((Boolean)invoke(r,"professionBusiness")),"current runner honors its failed-road pause");return path;}
 static void clear(Player p)throws Exception{try{invoke(singleton("population.quests.QuestRetryStore"),"clear",p.getObjectId());}catch(ClassNotFoundException expected){}}
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager"),life=singleton("offlinelife.OfflineLife");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"owner restores for durable profession retry");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);p.setAccessLevel(0);p.setIsInvul(false);
  if(Arrays.asList(args).contains("--resume")){
   check(p.getLevel()==19&&p.getClassId()==ClassId.ORC_FIGHTER,"second JVM restores the stored controlled profession actor");check(!((Boolean)invoke(runner(p),"professionBusiness")),"actual JVM restart keeps the existing failed-road pause");check(!((Boolean)invoke(life,"schedule",p))&&!((Boolean)invoke(life,"owns",p)),"blocked profession does not reclaim offline hunting");AutofarmManager.INSTANCE.startFarm(p);check(AutofarmManager.INSTANCE.isAutoFarming(p),"ordinary auto hunt can resume while the profession road is deferred");invoke(AutofarmManager.INSTANCE,"quiesce",p);
   int x=p.getX(),y=p.getY(),z=p.getZ();p.setXYZ(x+6000,y,z);check((Boolean)invoke(runner(p),"professionBusiness"),"a meaningfully different location permits a fresh route decision");p.setXYZ(x,y,z);clear(p);fail(p);
   try(java.sql.Connection c=net.sf.l2j.L2DatabaseFactory.getInstance().getConnection();java.sql.PreparedStatement q=c.prepareStatement("UPDATE autobot_quest_retry SET retry_at=? WHERE obj_Id=?")){q.setLong(1,System.currentTimeMillis()-1);q.setInt(2,p.getObjectId());q.executeUpdate();}check((Boolean)invoke(runner(p),"professionBusiness"),"expired retry allows another attempt without a real-time wait");
   clear(p);fail(p);p.setClassId(ClassId.ORC_MYSTIC.getId());check((Boolean)invoke(runner(p),"professionBusiness"),"another base class does not inherit the old branch failure");clear(p);
   System.out.println("PROFESSION RETRY RESUME ALL PASSED");System.exit(0);
  }
  p.setClassId(ClassId.ORC_FIGHTER.getId());p.setBaseClass(ClassId.ORC_FIGHTER);p.getStat().setLevel((byte)19);p.getStat().setExp(Experience.LEVEL[19]+1000);p.setXYZ(-28856,133144,-3664);clear(p);fail(p);check(!((Boolean)invoke(runner(p),"professionBusiness")),"a new runner retains the previous failed-road pause");
  if(Arrays.asList(args).contains("--plan-only")){System.out.println("PROFESSION RETRY PLAN ALL PASSED");System.exit(0);}
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Marusya");check(b!=null,"population actor exists for shared retry policy");b.setClassId(ClassId.ORC_FIGHTER.getId());b.setBaseClass(ClassId.ORC_FIGHTER);b.getStat().setLevel((byte)19);b.setAccessLevel(0);clear(b);fail(b);check(!((Boolean)invoke(runner(b),"professionBusiness")),"population runner also preserves the failed-road pause");clear(b);p.store();System.out.println("PROFESSION RETRY SAVE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
