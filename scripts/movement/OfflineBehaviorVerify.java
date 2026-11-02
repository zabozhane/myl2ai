import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Inventory planning works between combat loops and cannot reactivate an owner-controlled player. */
public class OfflineBehaviorVerify extends GatekeeperVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  @SuppressWarnings("unchecked") Map<Integer,Object> behaviors=(Map<Integer,Object>)member(AutofarmManager.INSTANCE,"combatBehaviors").get(AutofarmManager.INSTANCE);
  @SuppressWarnings("unchecked") Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);
  Object gear=singleton("population.economy.Gear");int tested=0;
  for(String name:new String[]{"Zenthen","Afrodita"}){
   Player p=World.getInstance().getPlayer(name);check(p!=null,"snapshot restores planning fixture "+name);
   invoke(AutofarmManager.INSTANCE,"quiesce",p);
   invoke(AutofarmManager.INSTANCE,"prepareOfflineBehavior",p);
   int shot=(Integer)invoke(gear,"shotId",p);boolean beginner=(Boolean)invoke(gear,"hasBeginnerShots",p);
   long exp=p.getExp();int cash=p.getAdena();AutofarmManager.INSTANCE.stopFarm(p,"planning fixture");behaviors.remove(p.getObjectId());
   check(!AutofarmManager.INSTANCE.isAutoFarming(p)&&!behaviors.containsKey(p.getObjectId()),name+" starts planning with combat stopped and cache absent");
   check(((Boolean)invoke(gear,"hasBeginnerShots",p))==beginner&&((Integer)invoke(gear,"shotId",p))==shot,name+" retains correct beginner shots and weapon-grade shot selection");
   check(behaviors.containsKey(p.getObjectId())&&!AutofarmManager.INSTANCE.isAutoFarming(p)&&cash==p.getAdena()&&exp==p.getExp(),name+" planning rebuilds only its profile without enabling combat or changing resources");
   farmers.remove(p.getObjectId());behaviors.remove(p.getObjectId());boolean rejected=false;
   try{invoke(gear,"hasBeginnerShots",p);}catch(RuntimeException expected){rejected=true;}
   check(rejected&&!behaviors.containsKey(p.getObjectId())&&!AutofarmManager.INSTANCE.isAutoFarming(p),name+" owner handoff cannot lazily restart offline behavior");
   farmers.put(p.getObjectId(),p);tested++;
  }
  check(tested==2,"both fighter and mage planning profiles verified");
  System.out.println("OFFLINE BEHAVIOR ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
