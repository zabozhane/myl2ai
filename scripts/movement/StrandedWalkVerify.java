import java.util.*;
import java.util.concurrent.*;
import kotlin.coroutines.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import dev.l2j.autobots.Autobot;

/** A nearby goal can require a much longer safe detour; exercise the real LifeBehavior transition. */
public class StrandedWalkVerify extends GatekeeperVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object j:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(j,"cancel");
  Autobot bot=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","sdf");check(bot!=null,"snapshot restores stranded sdf");
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",bot);
  ((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(bot.getName());
  warp(bot,new Location(9123,23994,-3544));bot.setAccessLevel(8);bot.setIsInvul(true);bot.getAppearance().setInvisible();
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(bot);invoke(bot,"setLife",life);
  Object spot=invoke(life,"currentSpot");check(spot!=null,"ordinary selector finds the recorded nearby prey");
  long scrolls=bot.getInventory().getInventoryItemCount(736,-1);int money=bot.getAdena();
  CountDownLatch done=new CountDownLatch(1);Object[] result=new Object[1];Continuation<Object> cont=new Continuation<Object>(){
   public CoroutineContext getContext(){return EmptyCoroutineContext.INSTANCE;}public void resumeWith(Object value){result[0]=value;done.countDown();}};
  Object value=invoke(life,"travelTo",spot,bot.isInsideZone(ZoneId.TOWN),cont);boolean suspended=value==kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
  long deadline=System.currentTimeMillis()+1000000;boolean recoveryChecked=false;
  while(suspended&&done.getCount()>0&&System.currentTimeMillis()<deadline){
   done.await(20,TimeUnit.SECONDS);if(done.getCount()==0)break;
   Object recovery=member(life,"recovery").get(life);
   if(recovery!=null&&!recoveryChecked){
    Object walk=invoke(recovery,"getWalk");routeValid(bot,(Location)invoke(recovery,"getOrigin"),walk,"wide recovery detour");
    check((Double)invoke(walk,"getLength")>30000,"local failure recovers through a real wide detour");
    check(invoke(life,"currentSpot")==spot,"successful recovery keeps its chosen prey instead of resetting selection");recoveryChecked=true;
    if(args.length>0&&args[0].equals("--plan-only")){System.out.println("STRANDED WALK PLAN ALL PASSED");System.exit(0);}
   }
   System.out.println("STRANDED WALK INFO at="+location(bot)+" recovery="+recoveryChecked+" hp="+(int)bot.getCurrentHp());
  }
  check(!suspended||done.getCount()==0,"recovery finishes within finite budget");if(suspended)kotlin.ResultKt.throwOnFailure(result[0]);
  check(recoveryChecked,"local route failure installs a validated recovery rather than abandoning prey");
  Object plan=member(life,"recovery").get(life);Location anchor=(Location)invoke(plan,"getAnchor");
  check(gap(location(bot),anchor)<=600,"stranded population bot actually reaches accessible prey");
  check(bot.getAdena()==money&&bot.getInventory().getInventoryItemCount(736,-1)==scrolls,"land recovery spends no invented cash or teleport scrolls");
  bot.setAccessLevel(0);bot.setIsInvul(false);bot.getAppearance().setVisible();long before=bot.getExp();deadline=System.currentTimeMillis()+180000;
  while(bot.getExp()<=before&&!bot.isDead()&&System.currentTimeMillis()<deadline){suspendValue(invoke(bot,"getCombatBehavior"),"onUpdate");Thread.sleep(1000);}
  check(bot.getExp()>before,"ordinary combat gains experience after the wide detour");
  System.out.println("STRANDED WALK ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
