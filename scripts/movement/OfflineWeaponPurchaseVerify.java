import java.util.*;
import kotlin.Pair;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Shop policy fixture: recorded inventory, controlled town position/wallet and real NPC transactions. */
public class OfflineWeaponPurchaseVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null&&p.getLevel()==15,"recorded level-15 buyer restores");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();p.setAccessLevel(0);p.setIsInvul(false);((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(m,"ensureQuestsLoaded",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  warp(p,new Location(9745,15606,-4496));check(p.isInsideZone(ZoneId.TOWN),"controlled policy fixture is in the actual dark-elven town");ItemInstance old=p.getInventory().getPaperdollItem(net.sf.l2j.gameserver.model.itemcontainer.Inventory.PAPERDOLL_RHAND);check(old!=null&&old.getItemId()==1,"recorded buyer still carries its starting Short Sword");
  Object market=singleton("population.economy.Market"),equipment=singleton("offlinelife.OfflineEquipment"),chores=singleton("offlinelife.OfflineChores");Pair<?,?> offer=(Pair<?,?>)invoke(market,"findProduct",p,3);check(offer!=null,"real local merchant sells Broadsword");int price=(Integer)invoke(market,"priceWithTax",offer.getFirst(),offer.getSecond());double before=(Double)invoke(equipment,"score",p,old);boolean purchaseOnly=Arrays.asList(args).contains("--purchase-only");
  int reserve=(Integer)invoke(singleton("population.economy.Errands"),"reserve",p);
  if(!purchaseOnly){
   int cash=p.getAdena();suspendValue(chores,"buyWeapon",p);check(p.getAdena()==cash&&p.getActiveWeaponItem().getItemId()==1,"recorded small wallet preserves supplies and buys no unaffordable weapon");
   check(invoke(equipment,"weaponPurchase",p,price-1)==null,"one adena below the actual taxed price rejects a purchase");check(invoke(equipment,"weaponPurchase",p,price)!=null,"the actual price boundary admits the fitting upgrade");
   money(p,price+reserve-1);cash=p.getAdena();suspendValue(chores,"buyWeapon",p);check(p.getAdena()==cash&&p.getActiveWeaponItem().getItemId()==1,"owned money reserved for supplies cannot fund a weapon purchase");
   int enchant=old.getEnchantLevel();old.setEnchantLevel(10);try{check(invoke(equipment,"weaponPurchase",p,Integer.MAX_VALUE)==null,"an enhanced weapon is protected from an automatic purchase");}finally{old.setEnchantLevel(enchant);}
   ItemInstance bow=p.getInventory().addItem("Policy fixture bow",13,1,p,null);invoke(singleton("population.economy.Gear"),"equip",p,bow);check(invoke(equipment,"weaponPurchase",p,Integer.MAX_VALUE)==null,"buying a kit sword cannot silently change an owner's chosen bow type");invoke(singleton("population.economy.Gear"),"equip",p,old);p.getInventory().destroyItem("Policy fixture end",bow.getObjectId(),1,p,null);
  }
  // Only the wallet is funded for this shop policy boundary; this is not claimed as earned adena.
  money(p,price+reserve+10000);
  invoke(equipment,"equip",p);List<ItemInstance> junk=(List<ItemInstance>)invoke(equipment,"surplus",p);long sale=0;for(ItemInstance item:junk)sale+=(long)(Integer)invoke(market,"npcSellPrice",item.getItemId())*item.getCount();
  int start=p.getAdena();long heals=p.getInventory().getInventoryItemCount(1060,-1),scrolls=p.getInventory().getInventoryItemCount(736,-1);long stones=p.getInventory().getInventoryItemCount(1475,-1)+p.getInventory().getInventoryItemCount(1476,-1);
  suspendValue(chores,"run",p);
  check(p.getActiveWeaponItem().getItemId()==3,"actual offline town chores buy and equip the useful local weapon");ItemInstance bought=p.getInventory().getPaperdollItem(net.sf.l2j.gameserver.model.itemcontainer.Inventory.PAPERDOLL_RHAND);check((Double)invoke(equipment,"score",p,bought)>before*1.2,"equipped weapon improves actual core attack stats");
  check(p.getAdena()==start+sale-price&&p.getAdena()>=reserve,"real NPC charges its taxed price and preserves the supply/travel reserve");check(p.getInventory().getInventoryItemCount(1060,-1)>=heals&&p.getInventory().getInventoryItemCount(736,-1)>=scrolls,"weapon purchase leaves carried healing and escape intact");check(p.getInventory().getInventoryItemCount(1475,-1)+p.getInventory().getInventoryItemCount(1476,-1)==stones,"weapon errand does not consume remote quest loot");check(p.getInventory().getItemByObjectId(old.getObjectId())!=null,"the owner's old weapon is retained after the purchase");
  if(!purchaseOnly){int cash=p.getAdena();suspendValue(chores,"buyWeapon",p);check(p.getAdena()==cash&&p.getInventory().getInventoryItemCount(3,-1)==1,"a satisfied weapon goal cannot buy another copy");}
  check(!p.isGM()&&!p.isInvul(),"ordinary shop rules and character access apply");System.out.println("OFFLINE WEAPON INFO price="+price+" sales="+sale+" cash="+start+"->"+p.getAdena()+" attack="+before+"->"+invoke(equipment,"score",p,bought));System.out.println("OFFLINE WEAPON PURCHASE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
