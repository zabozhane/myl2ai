import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Synthetic population decisions/rest state; the separate Zenthen field fixture preserves real resources. */
public class PopulationHealingVerify extends OfflineTownVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Object manager=singleton("AutobotsManager");Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Weapons");check(b!=null,"population actor restores");suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);b.abortAttack();b.abortCast();b.stopMove(null);b.setTarget(null);if(b.isSitting()){b.standUp();Thread.sleep(2800);}b.setAccessLevel(0);b.setIsInvul(false);
  for(int id:new int[]{1060,1061,1539,1540})erase(b,id);b.getInventory().addItem("isolated scarce-healing fixture",1060,1,b,null);money(b,304);b.refreshOverloaded();
  Object errands=singleton("population.economy.Errands");check(invoke(errands,"leaveReason",b,System.currentTimeMillis())==null,"population avoids a commute for one or two affordable potions");
  int price=(Integer)invoke(singleton("population.economy.Market"),"healingPriceAtHome",b);money(b,9*price);check("healing_stock".equals(invoke(errands,"leaveReason",b,System.currentTimeMillis())),"population still departs when a normal healing stock is funded");
  b.setCurrentHp(b.getMaxHp()*.70);Object rest=Class.forName("dev.l2j.autobots.behaviors.Rester").getConstructor(Player.class).newInstance(b);check(Boolean.TRUE.equals(invoke(rest,"onUpdate"))&&b.isSitting(),"scarce-healing population actor rests before risking its last potion");
  check(!b.isGM()&&!b.isInvul(),"population decision fixture uses no GM protection");System.out.println("POPULATION HEALING ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
