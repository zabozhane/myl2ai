import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.jvm.functions.Function1;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** A town job completing while the owner reenters cannot deadlock or resurrect a stale intent. */
public class OfflineTravelOwnershipVerify extends GatekeeperVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");
  OfflineTravelRestartVerify.pause(member(m,"travelTask").get(m));OfflineTravelRestartVerify.pause(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE));
  check(World.getInstance().getPlayer("Zenthen")==null,"controlled concurrency fixture has no automatic startup actions");Player p=Player.restore(268486152);check(p!=null,"fixture restores a real owner-controlled actor");
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);farmers.put(p.getObjectId(),p);invoke(m,"markFarming",p.getObjectId());invoke(m,"openJournal",p);
  Class<?> mode=Class.forName("dev.l2j.autobots.offlinelife.OfflineFarmManager$Trip"),type=Class.forName("dev.l2j.autobots.offlinelife.OfflineFarmManager$Travel");Object trip=Enum.valueOf((Class)mode,"TO_SPOT");Constructor<?> ctor=Arrays.stream(type.getDeclaredConstructors()).filter(c->c.getParameterCount()==21).findFirst().get();ctor.setAccessible(true);
  Object desired=ctor.newInstance(trip,-23689,142316,-3768,null,false,0,0,0,600,true,null,false,Collections.emptyList(),System.currentTimeMillis(),null,0,0L,Double.MAX_VALUE,0,0);
  Method setter=Arrays.stream(m.getClass().getDeclaredMethods()).filter(v->v.getName().equals("setTravel")&&v.getParameterCount()==2).findFirst().get();setter.setAccessible(true);Object identity=setter.getParameterTypes()[0]==int.class?p.getObjectId():p;
  CountDownLatch entered=new CountDownLatch(1);CompletableFuture<Boolean> done=new CompletableFuture<>();
  Function1<Continuation<? super Unit>,Object> action=(continuation)->{
   entered.countDown();try{setter.invoke(m,identity,desired);}catch(Exception e){throw new RuntimeException(e);}return Unit.INSTANCE;
  };
  Thread owner=new Thread(()->{try{
   synchronized(m){invoke(m,"townAction",p,action);if(!entered.await(3,TimeUnit.SECONDS))throw new AssertionError("town action did not enter");done.complete((Boolean)invoke(m,"onPlayerReentry",p));}
  }catch(Throwable t){done.completeExceptionally(t);}},"isolated-owner-reentry");owner.setDaemon(true);owner.start();
  boolean restored;try{restored=done.get(5,TimeUnit.SECONDS);}catch(TimeoutException e){throw new AssertionError("owner reentry waits forever on its own monitor while a town job completes",e);}
  check(restored,"owner reentry finishes while the town job is completing");Object store=singleton("offlinelife.OfflineTravelStore");check(invoke(store,"load",p.getObjectId())==null&&World.getInstance().getPlayer(p.getObjectId())==null,"owner handoff removes the completed/canceled job's intent and actor");
  Player replacement=Player.restore(p.getObjectId());farmers.put(p.getObjectId(),replacement);setter.invoke(m,identity,desired);
  check(invoke(store,"load",p.getObjectId())==null,"a late old actor cannot overwrite a replacement offline session");farmers.remove(p.getObjectId());
  System.out.println("OFFLINE TRAVEL OWNERSHIP ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
