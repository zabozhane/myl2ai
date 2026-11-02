import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.Unit;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Absolute hunt deadlines survive new legs; all elapsed time is injected. */
public class HuntJourneyVerify extends GatekeeperVerify{
 static Object journey(long budget,long cooldown)throws Exception{return Class.forName("dev.l2j.autobots.movement.HuntJourney").getConstructor(long.class,long.class).newInstance(budget,cooldown);}
 static Object expired()throws Exception{Object j=journey(900000,60000);member(j,"startedAt").setLong(j,System.currentTimeMillis()-900001);return j;}
 static void rules()throws Exception{
  Object j=journey(900000,60000);
  check(((Long)invoke(j,"deadline",0L))==900000,"first hunt begins a finite chain");
  check(((Long)invoke(j,"deadline",300000L))==900000,"next target does not restart chain budget");
  check(((Long)invoke(j,"deadline",899999L))==900000,"a progressing long recovery can use its allotted budget");
  check(((Long)invoke(j,"deadline",900000L))==0,"chain expires at the common deadline");
  check(((Long)invoke(j,"getRetryAt"))==960000,"expired chain has a finite retry time");
  check(((Long)invoke(j,"deadline",910000L))==0&&((Long)invoke(j,"getRetryAt"))==960000,"repeated ticks do not extend cooldown");
  check(((Long)invoke(j,"deadline",960000L))==1860000,"new attempt begins after cooldown");
  invoke(j,"reset");check(((Long)invoke(j,"deadline",970000L))==1870000,"productive hunt or completed purpose starts a fresh chain");
 }
 public static void main(String[] args){try{
  rules();if(args.length>0&&args[0].equals("--rules-only")){System.out.println("HUNT JOURNEY RULES ALL PASSED");return;}
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"offline fixture restored");AutofarmManager.INSTANCE.stopFarm(p,"journey verification");warp(p,new Location(-14056,121976,-2984));
  @SuppressWarnings("unchecked") Map<Integer,Object> journeys=(Map<Integer,Object>)member(m,"huntJourneys").get(m);journeys.put(p.getObjectId(),expired());
  for(String field:new String[]{"traveling","retryAfter"})((Map<?,?>)member(m,field).get(m)).clear();
  int money=p.getAdena();Location before=location(p);long start=System.currentTimeMillis();invoke(m,"goHunting",p);
  check(((Map<?,?>)member(m,"traveling").get(m)).isEmpty()&&((Map<?,?>)member(m,"retryAfter").get(m)).containsKey(p.getObjectId()),"expired offline chain defers another route");
  check(gap(before,location(p))==0&&money==p.getAdena()&&System.currentTimeMillis()-start<1000,"expired offline chain neither moves, pays nor searches again");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","BlackForce");suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);Object spot=invoke(life,"currentSpot");check(spot!=null,"population fixture has prey");
  member(life,"huntJourney").set(life,expired());before=location(b);suspendValue(life,"travelTo",spot,false);
  check(!b.isMoving()&&gap(before,location(b))==0,"expired population chain cannot start another leg");
  AtomicBoolean failed=new AtomicBoolean();Object walk=Class.forName("dev.l2j.autobots.behaviors.sequences.WalkToSequence")
    .getConstructor(Player.class,Location.class,int.class,kotlin.jvm.functions.Function0.class)
    .newInstance(p,new Location(p.getX()+10000,p.getY(),p.getZ()),20,(kotlin.jvm.functions.Function0<Unit>)()->{failed.set(true);return Unit.INSTANCE;});
  invoke(walk,"withHuntDeadline",System.currentTimeMillis()-1);suspendValue(walk,"definition");
  check(failed.get()&&"hunt_chain_timeout".equals(invoke(walk,"getFailureReason")),"shared walker enforces a caller's expired deadline before planning");
  System.out.println("HUNT JOURNEY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
