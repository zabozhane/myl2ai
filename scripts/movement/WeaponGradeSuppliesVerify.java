import java.util.*;
import java.util.concurrent.*;
import kotlin.Pair;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.player.Appearance;
import net.sf.l2j.gameserver.model.actor.player.Experience;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.actors.*;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Controlled Rogue/level/gear/wallet shop fixtures, actual NPC purchases; no earned-progression claim. */
public class WeaponGradeSuppliesVerify extends TownUnstuckVerify {
 static Object gear,market,offline;
 static void setup(Player p)throws Exception{
  p.setClassId(ClassId.ROGUE.getId());p.setBaseClass(ClassId.ROGUE);p.getStat().setLevel((byte)39);p.getStat().setExp(Experience.LEVEL[39]);p.setAccessLevel(0);p.setIsInvul(false);p.setTarget(null);
  for(int id:new int[]{13,279,17,1341,1835,1463,5789,5790,736,1060,1061,1539,1540})erase(p,id);
  ItemInstance bow=p.getInventory().addItem("Ammo policy setup",13,1,p,null);invoke(gear,"equip",p,bow);p.getInventory().addItem("Ammo policy setup",17,1500,p,null);p.getInventory().addItem("Ammo policy setup",736,2,p,null);p.getInventory().addItem("Ammo policy setup",1060,10,p,null);beside(p,npcId(30084));
  check(!p.isDead(),"controlled peaceful shop actor is alive");check(p.getActiveWeaponItem().getItemId()==13&&p.getInventory().getInventoryItemCount(1341,-1)==0,"fixture starts with a No Grade bow and only No Grade arrows");
 }
 static Pair<?,?> product(Player p,int id)throws Exception{Pair<?,?> q=(Pair<?,?>)invoke(market,"findProduct",p,id);check(q!=null,"actual local shop sells "+id);return q;}
 static int price(Pair<?,?> q)throws Exception{return (Integer)invoke(market,"priceWithTax",q.getFirst(),q.getSecond());}
 static void await(Object receiver,String method,Object... args)throws Exception{CompletableFuture<Object> f=async(receiver,method,args);f.get(300,TimeUnit.SECONDS);}
 static void verifyStock(Player p,String label)throws Exception{
  check(p.getActiveWeaponItem().getItemId()==279,label+" equips its paid D-grade bow");check(p.getInventory().getInventoryItemCount(1341,-1)>=500,label+" leaves with normal stock of D-grade arrows");check(((Integer)invoke(gear,"shotId",p))==1463,label+" requests shots matching the new D-grade weapon");if(invoke(market,"findProduct",p,1463)!=null)check(p.getInventory().getInventoryItemCount(1463,-1)>0,label+" pays for locally available matching shots");else check(p.getInventory().getInventoryItemCount(1463,-1)==0,label+" invents no unavailable D-grade shots");check(p.getInventory().getInventoryItemCount(1835,-1)==0,label+" does not buy shots for the old weapon grade");check(p.getAdena()>=(Integer)invoke(singleton("population.economy.Errands"),"reserve",p),label+" preserves its money reserve after arrows and shots");check(!p.isGM()&&!p.isInvul(),label+" uses normal character access");System.out.println("WEAPON GRADE INFO actor="+label+" arrows="+p.getInventory().getInventoryItemCount(1341,-1)+" shots="+p.getInventory().getInventoryItemCount(1463,-1)+" cash="+p.getAdena());
 }
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager"),manager=singleton("AutobotsManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");gear=singleton("population.economy.Gear");market=singleton("population.economy.Market");offline=singleton("offlinelife.OfflineChores");
  Autobot bot=new Autobot(IdFactory.getInstance().getNextId(),PlayerData.getInstance().getTemplate(ClassId.ROGUE),new Appearance((byte)0,(byte)0,(byte)0,Sex.MALE));bot.setName("AmmoPolicyPopulation");bot.setAccessLevel(0);bot.setIsInvul(false);bot.setXYZ(npcId(30084).getX()+40,npcId(30084).getY()+40,npcId(30084).getZ());bot.getStat().setLevel((byte)39);bot.getStat().setExp(Experience.LEVEL[39]);suspendValue(manager,"spawnAutobot",bot);((Map<?,?>)invoke(manager,"getActiveBots")).remove(bot.getName());invoke(bot,"cancelSequence");Thread.sleep(300);Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(bot);invoke(bot,"setLife",life);setup(bot);Object trip=Class.forName("dev.l2j.autobots.population.economy.TownTrip").getConstructor(Autobot.class,boolean.class).newInstance(bot,true);int bow=price(product(bot,279)),arrows=price(product(bot,1341));int reserve=(Integer)invoke(singleton("population.economy.Errands"),"reserve",bot);
  money(bot,bow+reserve);await(trip,"buyGear");check(bot.getActiveWeaponItem().getItemId()==13,"a population buyer cannot fund a new bow by spending its arrow stock money");if(Arrays.asList(args).contains("--guard-only")){System.out.println("WEAPON GRADE GUARD ALL PASSED");System.exit(0);}
  money(bot,bow+reserve+1500*arrows+50000);await(trip,"definition");verifyStock(bot,"population");
  Player p=Player.restore(268486152);check(p!=null,"offline owner restores for the controlled class/grade fixture");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(m,"ensureQuestsLoaded",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);setup(p);invoke(AutofarmManager.INSTANCE,"prepareOfflineBehavior",p);
  check(invoke(singleton("offlinelife.OfflineEquipment"),"weaponPurchase",p,bow)==null,"offline selection also protects the future arrow budget");money(p,bow+reserve+1500*arrows+50000);await(offline,"run",p);verifyStock(p,"offline");System.out.println("WEAPON GRADE SUPPLIES ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
