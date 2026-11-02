import java.util.*;
import kotlin.Pair;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.enums.ZoneId;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** One complete town-to-prey cycle from the current real snapshot, without GM or gifted resources. */
public class OfflineCycleVerify extends OfflineTownVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"fresh snapshot restores Zenthen");
  invoke(AutofarmManager.INSTANCE,"quiesce",p);p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
  @SuppressWarnings("unchecked") Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);farmers.entrySet().removeIf(e->e.getKey()!=p.getObjectId());
  check((Boolean)invoke(m,"isActive",p),"ordinary offline controller owns the snapshot character");
  long initialExp=p.getExp(),townExp=initialExp,departureExp=Long.MAX_VALUE;int initialMoney=p.getAdena();
  invoke(m,"startFarmLogged",p,"complete cycle verification");
  @SuppressWarnings("unchecked") Map<Integer,Pair<Long,Long>> progress=(Map<Integer,Pair<Long,Long>>)member(m,"huntProgress").get(m);
  // Reproduce the observed empty-anchor watchdog without waiting 45 real seconds.
  progress.put(p.getObjectId(),new Pair<>(p.getExp(),System.currentTimeMillis()-46000));
  boolean town=false,chores=false,leftTown=false,nudged=false,complete=false,previousTown=false;
  long started=System.currentTimeMillis(),deadline=started+1500000,reported=0;
  while(System.currentTimeMillis()<deadline){
   invoke(m,"tick");
   if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
   if(p.isDead())throw new AssertionError("Zenthen died during the complete cycle");
   boolean here=p.isInsideZone(ZoneId.TOWN),busy=hasTownAction(m,p);
   if(here&&!town){town=true;townExp=p.getExp();System.out.println("CYCLE INFO town entered exp="+townExp+" cash="+p.getAdena());}
   if(town&&busy)chores=true;
   if(town&&previousTown&&!here){leftTown=true;departureExp=p.getExp();}
   previousTown=here;
   if(!town&&!nudged&&p.getExp()>initialExp){
    // A real fight has happened. Accelerate only the optional city timers, never XP, loot or money.
    stamp(m,"lastTownTripAt",p,System.currentTimeMillis()-91*60000L);stamp(m,"productiveSince",p,System.currentTimeMillis()-11*60000L);stamp(m,"townCheckedAt",p,0);nudged=true;
    System.out.println("CYCLE INFO optional town timers advanced after actual hunting progress");
   }
   if(town&&chores&&leftTown&&!here&&p.getExp()>departureExp&&!busy&&
       !((Map<?,?>)member(m,"traveling").get(m)).containsKey(p.getObjectId())&&AutofarmManager.INSTANCE.isAutoFarming(p)){complete=true;break;}
   if(System.currentTimeMillis()-reported>=20000){
    reported=System.currentTimeMillis();Object trip=((Map<?,?>)member(m,"traveling").get(m)).get(p.getObjectId());
    System.out.println("CYCLE INFO elapsed="+(reported-started)/1000+" town="+town+" chores="+chores+" leftTown="+leftTown+" at="+location(p)+" exp="+p.getExp()+" cash="+p.getAdena()+" hp="+(int)p.getCurrentHp()+" mp="+(int)p.getCurrentMp()+" farm="+AutofarmManager.INSTANCE.isAutoFarming(p)+" trip="+(trip==null?"none":invoke(trip,"getTrip")+":"+invoke(trip,"getRouteIndex")+"/"+((List<?>)invoke(trip,"getRoute")).size()));
   }
   Thread.sleep(500);
  }
  check(complete,"Zenthen completes real town errands, leaves town and gains actual hunting XP");
  check(!p.isGM(),"complete cycle uses no GM protection");
  System.out.println("CYCLE INFO final exp="+p.getExp()+" initialExp="+initialExp+" money="+p.getAdena()+" initialMoney="+initialMoney+" elapsedSeconds="+(System.currentTimeMillis()-started)/1000);
  System.out.println("OFFLINE CYCLE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
