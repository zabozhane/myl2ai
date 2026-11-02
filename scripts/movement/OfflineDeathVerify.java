import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import dev.l2j.autobots.autofarm.AutofarmManager;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.manager.ZoneManager;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.zone.type.TownZone;
import net.sf.l2j.gameserver.model.actor.Npc;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/** Real core death and resurrection; only the 15-second wait age is substituted. */
public class OfflineDeathVerify extends OfflineTownVerify{
 static void townLandings(Player p)throws Exception{
  DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
  NodeList spawns=factory.newDocumentBuilder().parse(new File("data/xml/zones/TownZone.xml")).getElementsByTagName("spawn");int checked=0,grounded=0,unresolved=0;
  for(int i=0;i<spawns.getLength();i++){
   Element e=(Element)spawns.item(i);if("true".equals(e.getAttribute("isChaotic")))continue;
   Location raw=new Location(Integer.parseInt(e.getAttribute("x")),Integer.parseInt(e.getAttribute("y")),Integer.parseInt(e.getAttribute("z")));
   Location actual=(Location)invoke(MapRegionData.getInstance(),"groundTownSpawn",raw);
   check(raw.getX()==actual.getX()&&raw.getY()==actual.getY()&&Math.abs(raw.getZ()-actual.getZ())<=256,"town landing keeps its chosen XY and floor tolerance");
   int ground=GeoEngine.getInstance().getHeight(raw.getX(),raw.getY(),raw.getZ());
   boolean nearbyTown=Math.abs(ground-raw.getZ())<=256&&ZoneManager.getInstance().getZone(raw.getX(),raw.getY(),ground+5,TownZone.class)!=null;
   boolean originalTown=ZoneManager.getInstance().getZone(raw.getX(),raw.getY(),raw.getZ()+5,TownZone.class)!=null;
   if(nearbyTown||originalTown)check(ZoneManager.getInstance().getZone(actual.getX(),actual.getY(),actual.getZ()+5,TownZone.class)!=null,"eligible neutral landing remains recognized as a town");
   else{unresolved++;System.out.println("TOWN LANDING INFO outside town and not repairable by nearby height: "+raw);check(actual.getZ()==raw.getZ(),"unsupported landing is preserved rather than moved to another floor");}
   if(actual.getZ()!=raw.getZ())grounded++;checked++;
  }
  check(checked>20&&grounded>0,"all neutral XML town spawns checked, including corrected heights");
  for(String name:new String[]{"Papuma","Herbiel","Vollodos"}){
   beside(p,npc(name));p.teleportTo(MapRegionData.TeleportType.TOWN);p.onTeleported();
   check(p.isInsideZone(ZoneId.TOWN),"actual core return lands in recognized town near "+name);
  }
  System.out.println("TOWN LANDING INFO checked="+checked+" grounded="+grounded+" unresolved="+unresolved);
  System.out.println("TOWN LANDING ALL PASSED");
 }
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"snapshot restores offline death fixture");invoke(AutofarmManager.INSTANCE,"quiesce",p);
  @SuppressWarnings("unchecked") Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);farmers.entrySet().removeIf(e->e.getKey()!=p.getObjectId());
  ((Map<?,?>)member(m,"traveling").get(m)).clear();p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();townLandings(p);warp(p,new Location(-40824,-109208,-1808));
  long before=p.getExp();p.doDie(null);check(p.isDead(),"core death actually changes character state");invoke(m,"tick");
  check(p.isDead(),"resurrection does not ignore its waiting period");
  stamp(m,"diedAt",p,System.currentTimeMillis()-16000);invoke(m,"tick");Thread.sleep(150);
  check(!p.isDead()&&p.getCurrentHp()>0,"expired death wait performs actual resurrection without real-time waiting");
  check(p.isInsideZone(ZoneId.TOWN),"resurrected farmer arrives in a recognized town");
  check(hasTownAction(m,p)||((Map<?,?>)member(m,"traveling").get(m)).containsKey(p.getObjectId())||AutofarmManager.INSTANCE.isAutoFarming(p),"resurrection restores an owned next action");
  check(!p.isGM()&&p.getExp()<=before,"recovery uses no GM protection and preserves the normal death cost");
  cancelTown(m,p);Npc merchant=npc("Papuma");
  check(Boolean.TRUE.equals(suspendValue(singleton("population.economy.NpcTravel"),"reach",p,merchant,12000)),"resurrected character actually reaches the merchant");
  Object market=singleton("population.economy.Market");kotlin.Pair<?,?> quote=(kotlin.Pair<?,?>)invoke(market,"findProduct",p,1060);check(quote!=null,"town merchant offers real healing supplies");
  int price=(Integer)invoke(market,"priceWithTax",quote.getFirst(),quote.getSecond());int cash=p.getAdena();long potions=p.getInventory().getInventoryItemCount(1060,-1);
  check(Boolean.TRUE.equals(invoke(market,"buyFromNpc",p,quote.getFirst(),quote.getSecond(),1)),"resurrected character completes a real NPC purchase");
  check(p.getAdena()==cash-price&&p.getInventory().getInventoryItemCount(1060,-1)==potions+1,"post-death activity pays the real price and receives one potion");
  System.out.println("OFFLINE DEATH ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
