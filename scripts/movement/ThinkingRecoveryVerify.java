import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.coroutines.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Controller fault injection: an exception or cancellation cannot disable every later update. */
public class ThinkingRecoveryVerify extends GatekeeperVerify{
 static AtomicBoolean busy(Autobot b)throws Exception{return (AtomicBoolean)member(b,"isBusyThinking").get(b);}
 static Throwable root(Throwable t){while(t.getCause()!=null)t=t.getCause();return t;}
 @SuppressWarnings({"unchecked","rawtypes"}) static void activity(Object session,String name)throws Exception{
  Object mode=Enum.valueOf((Class)Class.forName("dev.l2j.autobots.population.SessionPlan$Activity"),name);invoke(session,"switchTo",mode,2,"isolated thinking recovery");
 }
 static CompletableFuture<Object> update(Autobot b,kotlinx.coroutines.Job job)throws Exception{
  CompletableFuture<Object> done=new CompletableFuture<>();Continuation<Object> cont=new Continuation<Object>(){
   public CoroutineContext getContext(){return job;}
   public void resumeWith(Object result){try{kotlin.ResultKt.throwOnFailure(result);done.complete(result);}catch(Throwable t){done.completeExceptionally(t);}}
  };
  Object v=invoke(b,"onUpdate",cont);if(v!=kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED())done.complete(v);return done;
 }
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);
  Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Weapons");check(b!=null,"snapshot restores population actor");
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());
  b.setAccessLevel(0);b.setIsInvul(false);b.getAppearance().setVisible();warp(b,new Location(-15303,124111,-3112));
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);Object session=member(life,"session").get(life);
  // A corrupt controller session forces a real exception after the thinking guard has been acquired.
  member(life,"session").set(life,null);Throwable fault=null;
  try{suspendValue(b,"onUpdate");}catch(Throwable t){fault=root(t);}finally{member(life,"session").set(life,session);}
  check(fault instanceof NullPointerException,"controller exception remains visible rather than being swallowed");
  check(!busy(b).get(),"controller exception releases the thinking guard");
  activity(session,"Afk");suspendValue(b,"onUpdate");check(b.isSitting(),"the next real update actually performs its requested rest action");
  warp(b,new Location(-15303,124111,-3112));activity(session,"Hunt");member(life,"tripDoneInTown").setBoolean(life,false);
  // Synthetic wallet/stock only force an awaited shop sequence, not combat success or economic gain.
  OfflineTownVerify.erase(b,736);for(int id:new int[]{1060,1061,1539,1540})OfflineTownVerify.erase(b,id);money(b,1000);
  kotlinx.coroutines.CompletableJob owner=kotlinx.coroutines.JobKt.Job(null);CompletableFuture<Object> pending=update(b,owner);
  check(!pending.isDone()&&busy(b).get(),"ordinary hunting update is awaiting a live town sequence");
  suspendValue(b,"onUpdate");check(busy(b).get(),"an overlapping tick does not release another update's guard");
  owner.cancel(new CancellationException("isolated sequence cancellation"));Throwable canceled=null;
  try{pending.get(10,TimeUnit.SECONDS);}catch(Throwable t){canceled=root(t);}
  check(canceled instanceof CancellationException,"structured cancellation propagates to the caller");
  check(!busy(b).get(),"cancellation releases the thinking guard");
  check(invoke(b,"getActiveSequence")==null,"canceled town sequence releases action ownership");
  b.stopMove(null);if(b.isSitting()){b.standUp();Thread.sleep(3000);}check(!b.isSitting(),"next action starts from an explicitly standing actor");
  activity(session,"Afk");suspendValue(b,"onUpdate");check(b.isSitting(),"ordinary behavior resumes after the canceled action");
  check(!b.isGM(),"all behavior checks use an ordinary population actor");
  System.out.println("THINKING RECOVERY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
