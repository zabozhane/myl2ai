import java.util.*;
import java.util.concurrent.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** A larger recovery allowance must retain a real narrow exit to nearby prey. */
public class RecoveryNarrowExitVerify extends TownUnstuckVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);
  Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");for(Object j:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(j,"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Object manager=singleton("AutobotsManager");Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","rony");check(b!=null,"snapshot restores stranded Dark Mystic");Location from=location(b);check(gap(from,new Location(12168,16680,-4584))<30,"recorded narrow village origin");
  suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);b.stopMove(null);check(gap(from,location(b))<30,"setup does not move recorded actor");check(!b.isGM()&&!b.isInvul(),"ordinary actor has no GM protection");
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);
  Object spot=invoke(life,"currentSpot");check(spot!=null,"ordinary population selector recovers nearby prey from narrow origin");Object rec=member(life,"recovery").get(life);check(rec!=null,"selector installs a real validated recovery");Object route=invoke(rec,"getWalk");Location goal=(Location)invoke(rec,"getAnchor");routeValid(b,from,route,"nearby recovery narrow exit");check((Double)invoke(route,"getLength")>10000,"exit uses genuine long walking route");check(((List<?>)invoke(singleton("population.economy.GatekeeperTravel"),"offers",b)).isEmpty(),"actor cannot afford a protected-reserve ride");
  if(args.length>0&&args[0].equals("--plan-only")){System.out.println("RECOVERY NARROW EXIT PLAN ALL PASSED");System.exit(0);}
  long scrolls=b.getInventory().getInventoryItemCount(736,-1),xp=b.getExp();int money=b.getAdena();long began=System.currentTimeMillis();
  CompletableFuture<Object> travel=async(life,"travelTo",spot,b.isInsideZone(ZoneId.TOWN));while(!travel.isDone()&&System.currentTimeMillis()-began<600000){Thread.sleep(1000);if((System.currentTimeMillis()-began)/1000%20==0)System.out.println("RECOVERY NARROW INFO seconds="+(System.currentTimeMillis()-began)/1000+" at="+location(b)+" hp="+(int)b.getCurrentHp());}
  check(travel.isDone(),"real walk finishes within original finite budget");travel.get();check(!b.isDead()&&gap(location(b),goal)<=650,"ordinary actor physically reaches selected prey");check(b.getAdena()==money&&b.getInventory().getInventoryItemCount(736,-1)==scrolls,"walk neither pays a fake fare nor consumes SoE");
  long deadline=System.currentTimeMillis()+180000;while(b.getExp()<=xp&&!b.isDead()&&System.currentTimeMillis()<deadline){suspendValue(invoke(b,"getCombatBehavior"),"onUpdate");Thread.sleep(500);}
  check(b.getExp()>xp,"ordinary mage gains real experience after leaving village");System.out.println("RECOVERY NARROW INFO xp="+xp+"->"+b.getExp()+" seconds="+(System.currentTimeMillis()-began)/1000);System.out.println("RECOVERY NARROW EXIT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
