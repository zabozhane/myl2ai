import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Check the same NPC approach/departure through the population actor, without GM protection. */
public class NpcPopulationVerify extends NpcReturnVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);
  Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Weapons");check(b!=null,"snapshot restores population NPC actor");
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());
  b.setAccessLevel(0);b.setIsInvul(false);b.getAppearance().setVisible();Location entry=new Location(-15303,124111,-3107);exact(b,entry);
  long scrolls=b.getInventory().getInventoryItemCount(736,-1);int cash=b.getAdena();Object nav=singleton("population.economy.NpcTravel");Npc harmony=npc("Harmony");
  reversible(b,location(b),invoke(nav,"plan",b,harmony,location(b),12000),"population Harmony approach");
  check(Boolean.TRUE.equals(suspendValue(nav,"reach",b,harmony,12000)),"population walker actually reaches Harmony");
  check(!(Boolean)invoke(singleton("movement.TownUnstuck"),"isSealed",b),"population NPC arrival has a way out");
  Object exit=invoke(nav,"returnableToPoint",b,location(b),entry,12000);reversible(b,location(b),exit,"population shop departure");walk(b,entry,exit);
  check(b.getAdena()==cash&&b.getInventory().getInventoryItemCount(736,-1)==scrolls,"population approach/departure uses no invented cash or escape");
  check(!b.isGM(),"population navigation uses an ordinary actor");
  System.out.println("NPC POPULATION RETURN ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
