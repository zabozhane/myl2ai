import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** The real update dispatcher cannot release another update's guard after a pre-claim failure. */
public class ThinkingDispatcherVerify extends GatekeeperVerify{
 @SuppressWarnings({"unchecked","rawtypes"}) public static void main(String[] args){PrintStream original=System.err;try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object offline=singleton("offlinelife.OfflineFarmManager");invoke(member(offline,"travelTask").get(offline),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Object manager=singleton("AutobotsManager");Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Weapons");check(b!=null,"snapshot restores dispatcher actor");suspendValue(manager,"spawnAutobot",b);Map<String,Autobot> active=(Map<String,Autobot>)invoke(manager,"getActiveBots");active.remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);b.stopMove(null);warp(b,new Location(-15303,124111,-3112));b.setAccessLevel(0);b.setIsInvul(false);b.getAppearance().setVisible();
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);Object session=member(life,"session").get(life);Object afk=Enum.valueOf((Class)Class.forName("dev.l2j.autobots.population.SessionPlan$Activity"),"Afk");invoke(session,"switchTo",afk,2,"isolated dispatcher recovery");
  AtomicBoolean busy=(AtomicBoolean)member(b,"isBusyThinking").get(b);Object remembered=member(b,"previousIterationLocation").get(b);ByteArrayOutputStream errors=new ByteArrayOutputStream();PrintStream capture=new PrintStream(errors,true,"UTF-8");
  // Simulate a guard already owned by another update, and a failure before this tick acquires it.
  busy.set(true);member(b,"previousIterationLocation").set(b,null);System.setErr(capture);active.put(b.getName(),b);
  long until=System.currentTimeMillis()+5000;
  while(System.currentTimeMillis()<until&&!errors.toString("UTF-8").contains("AutobotsManager"))Thread.sleep(20);
  active.remove(b.getName());Thread.sleep(500);member(b,"previousIterationLocation").set(b,remembered);System.setErr(original);capture.close();
  check(errors.toString("UTF-8").contains("NullPointerException")&&errors.toString("UTF-8").contains("AutobotsManager"),"pre-claim error reaches the actual live dispatcher and remains reported");
  check(busy.get(),"failed unrelated tick cannot release the current owner's thinking guard");
  busy.set(false);suspendValue(b,"onUpdate");check(b.isSitting(),"after its owner releases the guard ordinary behavior still executes");check(!b.isGM(),"dispatcher verification uses no GM protection");
  System.out.println("THINKING DISPATCHER ALL PASSED");System.exit(0);
 }catch(Throwable t){System.setErr(original);t.printStackTrace();System.exit(1);}}
}
