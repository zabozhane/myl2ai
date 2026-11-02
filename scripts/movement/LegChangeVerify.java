import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.Unit;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Reproduce the observed rounded intermediate cell after the original ray was planned. */
public class LegChangeVerify extends NpcReturnVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);
  Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  check(World.getInstance().getPlayer("Zenthen")==null,"fixture suppresses automatic offline actions");Player p=Player.restore(268486152);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
  Location start=new Location(-14024,122584,-3112),mid=new Location(-14032,122952,-3112),goal=new Location(-14040,123272,-3112);GeoEngine g=GeoEngine.getInstance();
  check(g.canMoveToTarget(start.getX(),start.getY(),start.getZ(),goal.getX(),goal.getY(),goal.getZ()),"original full ray passes geodata");
  check(!g.canMoveToTarget(mid.getX(),mid.getY(),mid.getZ(),goal.getX(),goal.getY(),goal.getZ()),"recorded intermediate cell invalidates the ray");exact(p,start);
  AtomicBoolean failed=new AtomicBoolean(false);Object sequence=Class.forName("dev.l2j.autobots.behaviors.sequences.WalkToSequence").getConstructor(Player.class,Location.class,int.class,kotlin.jvm.functions.Function0.class)
   .newInstance(p,goal,20,(kotlin.jvm.functions.Function0<Unit>)()->{failed.set(true);return Unit.INSTANCE;});
  CompletableFuture<Object> done=async(sequence,"definition");check(!done.isDone(),"original walk starts before the intermediate-cell replay");
  // Fixture-only position injection reproduces the exact observed live rounding; all subsequent movement is real.
  exact(p,mid);done.get(45,TimeUnit.SECONDS);
  check(!failed.get()&&gap(location(p),goal)<=20,"changed leg replans from actual position and physically reaches its goal");
  check(!p.isGM(),"leg recovery uses no GM protection");System.out.println("LEG CHANGE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
