import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;
/** A failed hunt route on ordinary connected ground must not become an escape. */
public class FieldRecoveryEligibilityVerify extends GatekeeperVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");Object manager=singleton("AutobotsManager");
  String[] names={"Marusya","Mapgyk"};Location[] at={new Location(-83761,243620,-3728),new Location(22072,18648,-3536)};
  for(int i=0;i<names.length;i++){Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName",names[i]);check(b!=null,"recorded connected-ground actor restores");suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);b.abortAttack();b.abortCast();b.stopMove(null);b.setTarget(null);b.setAccessLevel(0);b.setIsInvul(false);warp(b,at[i]);check(Boolean.FALSE.equals(invoke(singleton("movement.TownUnstuck"),"needsRecovery",b)),names[i]+" ordinary connected ground does not justify an escape after path failure");check(!b.isCastingNow(),"negative eligibility check starts no cast");}
  System.out.println("FIELD RECOVERY ELIGIBILITY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
