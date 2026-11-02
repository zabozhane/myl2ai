import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Small-wallet shop fixtures use actual NPC prices/transactions; no road is repeated. */
public class SupplyBudgetVerify extends OfflineTownVerify{
 static void rules()throws Exception{
  Object b=singleton("movement.SupplyBudget");
  check(((Integer)invoke(b,"healingTarget",1000,0,103,460,false))==5,"1000 adena covers five heals and one emergency escape");
  check(((Integer)invoke(b,"healingTarget",500,0,103,460,false))==4,"insufficient combined budget still prioritizes healing");
  check(((Integer)invoke(b,"healingTarget",1000,0,103,460,true))==9,"owned escape is not reserved twice");
  check(((Integer)invoke(b,"healingTarget",770,0,103,460,false))==3,"minimum three heals are protected before escape");
  check(((Integer)invoke(b,"healingTarget",1000,2,103,460,false))==7,"existing heals count toward the minimum");
  check(((Integer)invoke(b,"healingTarget",0,0,103,460,false))==0,"empty wallet cannot fund invented supplies");
  check(((Integer)invoke(b,"healingTarget",Integer.MAX_VALUE,0,103,460,false))==10,"large wallet remains capped at ordinary healing stock");
 }
 static void prepare(Player p,int cash)throws Exception{
  for(int id:new int[]{736,1060,1061,1539,1540,17})erase(p,id);money(p,cash);
  check((Integer)invoke(singleton("population.economy.Healing"),"count",p)==0,"fixture starts without healing stock");
 }
 static int price(Player p,int id)throws Exception{
  Object market=singleton("population.economy.Market");kotlin.Pair<?,?> q=(kotlin.Pair<?,?>)invoke(market,"findProduct",p,id);check(q!=null,"NPC sells fixture item "+id);return (Integer)invoke(market,"priceWithTax",q.getFirst(),q.getSecond());
 }
 static void buy(Player p,Object trip)throws Exception{if(trip==null)suspendValue(singleton("offlinelife.OfflineChores"),"buySupplies",p);else suspendValue(trip,"buySupplies");}
 static void shopping(Player p,Object trip,String label)throws Exception{
  beside(p,npc("Papuma"));p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
  check(!(Boolean)invoke(singleton("population.economy.Gear"),"isArcher",p),label+" starts with a non-bow fixture");
  prepare(p,1000);int hp=price(p,1060),escape=price(p,736);buy(p,trip);
  long heals=p.getInventory().getInventoryItemCount(1060,-1),scrolls=p.getInventory().getInventoryItemCount(736,-1);
  check(heals>=3&&scrolls==1,label+" buys survival stock and one escape with a small wallet");
  check(p.getAdena()==1000-heals*hp-escape,label+" pays exact real NPC prices without created money");
  prepare(p,0);buy(p,trip);check(p.getAdena()==0&&p.getInventory().getInventoryItemCount(1060,-1)==0&&p.getInventory().getInventoryItemCount(736,-1)==0,label+" empty wallet buys nothing");
  ItemInstance bow=p.getInventory().getItemByItemId(13);if(bow==null)bow=p.getInventory().addItem("Verify fixture bow",13,1,p,null);
  invoke(singleton("population.economy.Gear"),"equip",p,bow);check((Boolean)invoke(singleton("population.economy.Gear"),"isArcher",p),label+" bow fixture is equipped");
  prepare(p,1000);int arrow=price(p,17);buy(p,trip);
  heals=p.getInventory().getInventoryItemCount(1060,-1);scrolls=p.getInventory().getInventoryItemCount(736,-1);long arrows=p.getInventory().getInventoryItemCount(17,-1);
  check(heals>=3&&scrolls==1&&arrows>=50,label+" bulk arrows cannot consume the emergency escape budget");
  check(p.getAdena()==1000-heals*hp-escape-arrows*arrow,label+" bow shopping pays all actual prices");
 }
 public static void main(String[] args){try{
  boolean npcOnly=args.length>0&&args[0].equals("--npc-only");if(!npcOnly)rules();
  if(args.length>0&&args[0].equals("--rules-only")){System.out.println("SUPPLY BUDGET RULES ALL PASSED");return;}
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"snapshot restores offline buyer");invoke(AutofarmManager.INSTANCE,"quiesce",p);shopping(p,null,"offline buyer");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Weapons");check(b!=null,"snapshot restores population buyer");
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());
  Object trip=Class.forName("dev.l2j.autobots.population.economy.TownTrip").getConstructor(Autobot.class,boolean.class).newInstance(b,false);shopping(b,trip,"population buyer");
  System.out.println("SUPPLY BUDGET ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
