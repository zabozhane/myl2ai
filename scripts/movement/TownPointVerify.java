import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import dev.l2j.autobots.Autobot;

/** Live: Haton read a Scroll of Escape every ~70 s standing at the Orc village teleport point (-44968,-113592). */
public class TownPointVerify extends OfflineTownVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Haton");check(b!=null,"Haton restores");
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);
  warp(b,new Location(-44968,-113592,-192));
  long soe=b.getInventory().getInventoryItemCount(736,-1);System.out.println("TOWN POINT INFO townZone="+b.isInsideZone(ZoneId.TOWN)+" soe="+soe);
  check(soe>0,"Haton owns scrolls");
  Object seq=Class.forName("dev.l2j.autobots.population.economy.GoToTownSequence").getConstructors()[0].newInstance(b,40000,"session_or_party",(kotlin.jvm.functions.Function0<kotlin.Unit>)()->kotlin.Unit.INSTANCE);
  suspendValue(seq,"definition");Thread.sleep(500);
  long after=b.getInventory().getInventoryItemCount(736,-1);System.out.println("TOWN POINT INFO soe="+soe+"->"+after+" at="+location(b));
  check(after==soe,"no scroll is read at the town's own teleport point");
  System.out.println("TOWN POINT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
