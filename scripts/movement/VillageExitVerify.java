import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.Unit;
import kotlin.coroutines.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;

/** Exact stationary village cases: safe route construction and an actual shared walk. */
public class VillageExitVerify extends GatekeeperVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object manager=singleton("offlinelife.OfflineFarmManager");invoke(member(manager,"travelTask").get(manager),"cancel");
  for(Object job:((Map<?,?>)member(manager,"townJobs").get(manager)).values())invoke(job,"cancel");
  Object planner=singleton("population.economy.NpcTravel");
  String[] names={"BlackForce","JetixmAX"};
  int[][] origins={{12116,17389,-4495},{11336,14344,-4240}};
  Location[] goals={new Location(21200,18942,-3536),new Location(21200,18942,-3536)};
  Autobot walker=null;Location destination=null;
  for(int i=0;i<names.length;i++){
   Autobot bot=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName",names[i]);check(bot!=null,"snapshot contains "+names[i]);
   suspendValue(singleton("AutobotsManager"),"spawnAutobot",bot);
   // The fixture drives only this task; prevent the population updater from issuing concurrent errands.
   ((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(bot.getName());
   warp(bot,new Location(origins[i][0],origins[i][1],origins[i][2]));
   long now=System.currentTimeMillis();Object route=invoke(planner,"toPoint",bot,location(bot),goals[i],15000);
   routeValid(bot,location(bot),route,names[i]+" village exit");
   check(System.currentTimeMillis()-now<5000,names[i]+" planning stays bounded");
   if(i==0){walker=bot;destination=goals[i];}else bot.decayMe();
  }
  if(args.length>0&&args[0].equals("--plan-only")){System.out.println("VILLAGE EXIT PLAN ALL PASSED");System.exit(0);}
  final Autobot bot=walker;bot.setAccessLevel(8);bot.setIsInvul(true);bot.getAppearance().setInvisible();
  AtomicBoolean failed=new AtomicBoolean();Object sequence=Class.forName("dev.l2j.autobots.behaviors.sequences.WalkToSequence")
   .getConstructor(Player.class,Location.class,int.class,kotlin.jvm.functions.Function0.class)
   .newInstance(bot,destination,100,(kotlin.jvm.functions.Function0<Unit>)()->{failed.set(true);return Unit.INSTANCE;});
  CountDownLatch done=new CountDownLatch(1);Object[] result=new Object[1];Continuation<Object> cont=new Continuation<Object>(){
   public CoroutineContext getContext(){return EmptyCoroutineContext.INSTANCE;}public void resumeWith(Object value){result[0]=value;done.countDown();}};
  Object value=invoke(sequence,"definition",cont);boolean suspended=value==kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED();
  long deadline=System.currentTimeMillis()+360000;
  while(suspended&&done.getCount()>0&&System.currentTimeMillis()<deadline){done.await(20,TimeUnit.SECONDS);System.out.println("EXIT INFO at="+location(bot)+" distance="+(int)gap(location(bot),destination));}
  check(!suspended||done.getCount()==0,"shared walker finishes within finite test budget");if(suspended)kotlin.ResultKt.throwOnFailure(result[0]);
  check(!failed.get()&&gap(location(bot),destination)<=100,"shared walker actually exits village and reaches hunting ground");
  bot.setAccessLevel(0);bot.setIsInvul(false);bot.getAppearance().setVisible();long before=bot.getExp();deadline=System.currentTimeMillis()+180000;
  while(bot.getExp()<=before&&!bot.isDead()&&System.currentTimeMillis()<deadline){suspendValue(invoke(bot,"getCombatBehavior"),"onUpdate");Thread.sleep(1000);}
  check(bot.getExp()>before,"normal combat earns experience after arrival");
  System.out.println("VILLAGE EXIT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
