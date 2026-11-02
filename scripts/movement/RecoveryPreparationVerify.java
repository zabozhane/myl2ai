import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Real stuck snapshot: recover, complete missed services, then actually hunt without GM/gifts/warps. */
public class RecoveryPreparationVerify extends TownUnstuckVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();boolean prepOnly=Arrays.asList(args).contains("--prep-only");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  check(World.getInstance().getPlayer("Zenthen")==null,"controlled fixture suppresses automatic startup actions");Player p=Player.restore(268486152);check(p!=null,"real stuck snapshot restores Zenthen");
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);
  check(p.getX()==-15048&&p.getY()==124424,"offline actor starts in the exact sealed live cell");Object support=singleton("population.economy.NewbieSupport");
  check((Boolean)invoke(support,"needs",p),"the failed original town visit left actual novice support missing");
  Location origin=location(p);long initialExp=p.getExp(),departureExp=Long.MAX_VALUE;int initialMoney=p.getAdena();long started=System.currentTimeMillis(),deadline=started+900000,reported=0;boolean recovered=false,prepared=false,complete=false;
  invoke(m,"goHunting",p);
  while(System.currentTimeMillis()<deadline){
   invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
   if(gap(origin,location(p))>32&&!p.isTeleporting())recovered=true;
   if(recovered&&p.isInsideZone(ZoneId.TOWN)&&!(Boolean)invoke(support,"needs",p)&&p.getInventory().getInventoryItemCount(736,-1)>=1&&((Integer)invoke(singleton("population.economy.Healing"),"count",p))>=3){
    if(!prepared){
     prepared=true;System.out.println("PREPARATION INFO ready at="+location(p)+" cash="+p.getAdena()+" maxHp="+(int)p.getMaxHp()+" elapsed="+(System.currentTimeMillis()-started)/1000);
     check(!AutofarmManager.INSTANCE.isAutoFarming(p),"city preparation owns the actor while actual support and supplies are received");
    }
    if(prepOnly){System.out.println("RECOVERY PREPARATION ONLY ALL PASSED");System.exit(0);}
   }
   Object travel=((Map<?,?>)member(m,"traveling").get(m)).get(p.getObjectId());
   if(recovered&&!prepared&&travel!=null&&invoke(travel,"getTrip").toString().equals("TO_SPOT"))throw new AssertionError("hunting trip starts before missed town preparation is completed");
   if(p.isDead()){
    for(Creature c:p.getKnownTypeInRadius(Creature.class,1500))if(c.getTarget()==p)System.out.println("PREPARATION INFO death attacker="+c.getName()+" lv="+c.getLevel()+" at="+c.getX()+","+c.getY()+","+c.getZ());
    throw new AssertionError("death before recovered/prepared actor gains hunting XP");
   }
   if(prepared&&!p.isInsideZone(ZoneId.TOWN)&&departureExp==Long.MAX_VALUE)departureExp=p.getExp();
   if(prepared&&!p.isInsideZone(ZoneId.TOWN)&&p.getExp()>departureExp&&travel==null&&!hasTownAction(m,p)&&AutofarmManager.INSTANCE.isAutoFarming(p)){complete=true;break;}
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();System.out.println("PREPARATION INFO elapsed="+(reported-started)/1000+" recovered="+recovered+" prepared="+prepared+" at="+location(p)+" hp="+(int)p.getCurrentHp()+"/"+(int)p.getMaxHp()+" exp="+p.getExp()+" cash="+p.getAdena()+" town="+p.isInsideZone(ZoneId.TOWN)+" job="+hasTownAction(m,p)+" trip="+(travel==null?"none":invoke(travel,"getTrip")));}
   Thread.sleep(500);
  }
  check(complete,"normal offline sequence completes recovery, genuine services, arrival and real hunting XP");check(!p.isGM(),"complete recovery/preparation/hunt cycle uses no GM protection");
  System.out.println("PREPARATION INFO finalExp="+p.getExp()+" initialExp="+initialExp+" finalMoney="+p.getAdena()+" initialMoney="+initialMoney);
  // A separate synthetic population fixture replays a previously marked-complete failed town stay.
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Weapons");check(b!=null,"population fixture exists");suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());
  b.setAccessLevel(0);b.setIsInvul(false);b.getAppearance().setVisible();warp(b,origin);if(b.getInventory().getInventoryItemCount(736,-1)==0)b.getInventory().addItem("Isolated population recovery fixture",736,1,b,null);
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);member(life,"tripDoneInTown").setBoolean(life,true);
  async(life,"hunt").get(60,java.util.concurrent.TimeUnit.SECONDS);
  check(!(Boolean)invoke(singleton("movement.TownUnstuck"),"isSealed",b),"population recovery really leaves the sealed cell");
  check(!member(life,"tripDoneInTown").getBoolean(life),"population retries its failed preparation instead of treating the old town stay as complete");
  System.out.println("RECOVERY PREPARATION ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
