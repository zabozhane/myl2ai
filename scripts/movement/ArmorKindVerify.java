import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.items.ArmorType;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import dev.l2j.autobots.Autobot;

/**
 * Planning only, on fresh in-memory characters in Gludio (nothing saved): the class kit decides heavy/light/robe,
 * the starting Squire's light set is replaced by the class's kind, and leftovers of the wrong kind become surplus.
 */
public class ArmorKindVerify extends OfflineTownVerify {
 static final int SQUIRE_SHIRT=1146,SQUIRE_PANTS=1147,BRONZE_BREASTPLATE=26,BRONZE_GAITERS=34,HARD_LEATHER_SHIRT=27;
 static Object eq,gear;
 static Player character(ClassId cls,int level,int... worn)throws Exception{
  Class<?> h=Class.forName("dev.l2j.autobots.utils.AutobotHelpersKt");Object app=null;
  for(java.lang.reflect.Method m:h.getDeclaredMethods())if(m.getName().startsWith("getRandomAppearance")){m.setAccessible(true);app=m.invoke(null,PlayerData.getInstance().getTemplate(cls).getRace());}
  Autobot b=new Autobot(IdFactory.getInstance().getNextId(),PlayerData.getInstance().getTemplate(cls),(net.sf.l2j.gameserver.model.actor.player.Appearance)app);
  b.setBaseClass(cls);b.getStat().setLevel((byte)level);b.setXYZInvisible(-14225,123540,-3121);
  for(int id:worn)b.getInventory().equipItemAndRecord(b.getInventory().addItem("Verify",id,1,b,null));
  return b;
 }
 static ArmorType kind(Item it){return it.getItemType() instanceof ArmorType?(ArmorType)it.getItemType():null;}
 static boolean body(Item it){ArmorType k=kind(it);return k==ArmorType.HEAVY||k==ArmorType.LIGHT||k==ArmorType.MAGIC;}
 /** Buys the plans one by one (items are added, not paid) and returns the kinds of body armor bought. */
 static Set<ArmorType> follow(String label,Player p,int budget)throws Exception{
  Set<ArmorType> kinds=new TreeSet<>();
  for(int i=0;i<8;i++){
   kotlin.Pair<?,?> r=(kotlin.Pair<?,?>)invoke(eq,"armorPlan",p,budget);Object plan=r.getFirst();if(plan==null)break;
   Item it=(Item)invoke(invoke(plan,"getProduct"),"getItem");
   System.out.println("ARMOR KIND INFO "+label+" lv="+p.getLevel()+" plan="+it.getName()+" type="+it.getItemType()+" gain="+invoke(plan,"getGain")+" price="+invoke(plan,"getPrice"));
   if(body(it))kinds.add(kind(it));
   ItemInstance bought=p.getInventory().addItem("Verify",it.getItemId(),1,p,null);p.getInventory().equipItemAndRecord(bought);
  }
  return kinds;
 }
 static ArmorType chest(Player p){ItemInstance c=p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);return c==null?null:kind(c.getItem());}
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);eq=singleton("offlinelife.OfflineEquipment");gear=singleton("population.economy.Gear");

  Player newbie=character(ClassId.HUMAN_FIGHTER,12,SQUIRE_SHIRT,SQUIRE_PANTS);
  check(invoke(gear,"armorKind",newbie)==ArmorType.HEAVY,"fighter kit asks for heavy armor");
  Set<ArmorType> k=follow("fighter",newbie,80000);
  check(k.equals(EnumSet.of(ArmorType.HEAVY)),"fighter in Squire's set plans only heavy body armor: "+k);
  check(chest(newbie)==ArmorType.HEAVY,"fighter ends in a heavy chest");

  Player knight=character(ClassId.KNIGHT,25,HARD_LEATHER_SHIRT,SQUIRE_PANTS);
  k=follow("knight",knight,600000);
  check(k.equals(EnumSet.of(ArmorType.HEAVY)),"knight in light armor plans only heavy body armor: "+k);
  check(chest(knight)==ArmorType.HEAVY,"knight ends in a heavy chest");
  // Squire's pieces aren't sellable at all; a bought light shirt is.
  List<?> surplus=(List<?>)invoke(eq,"surplus",knight);
  check(surplus.stream().anyMatch(i->((ItemInstance)i).getItemId()==HARD_LEATHER_SHIRT),"light shirt becomes surplus after the heavy chest");
  check(surplus.stream().noneMatch(i->kind(((ItemInstance)i).getItem())==ArmorType.HEAVY),"heavy pieces are never surplus");

  Player rogue=character(ClassId.ROGUE,25,SQUIRE_SHIRT,SQUIRE_PANTS);
  check(invoke(gear,"armorKind",rogue)==ArmorType.LIGHT,"rogue kit asks for light armor");
  k=follow("rogue",rogue,600000);
  check(!k.isEmpty()&&k.equals(EnumSet.of(ArmorType.LIGHT)),"rogue keeps light armor: "+k);

  Player mage=character(ClassId.HUMAN_WIZARD,25,425,461);
  k=follow("wizard",mage,600000);
  check(!k.isEmpty()&&k.equals(EnumSet.of(ArmorType.MAGIC)),"wizard keeps robes: "+k);

  // Population: loot of the class's kind replaces the starting light set, never the other way round.
  Player looter=character(ClassId.HUMAN_FIGHTER,12,SQUIRE_SHIRT,SQUIRE_PANTS);
  ItemInstance heavy=looter.getInventory().addItem("Verify",BRONZE_BREASTPLATE,1,looter,null);
  check((Boolean)invoke(gear,"isUpgrade",looter,heavy),"population: heavy loot replaces the Squire's shirt");
  looter.getInventory().equipItemAndRecord(heavy);
  ItemInstance light=looter.getInventory().addItem("Verify",HARD_LEATHER_SHIRT,1,looter,null);
  check(!(Boolean)invoke(gear,"isUpgrade",looter,light),"population: light loot never replaces the heavy chest");
  List<?> wishes=(List<?>)invoke(gear,"wishes",character(ClassId.HUMAN_FIGHTER,12,SQUIRE_SHIRT,SQUIRE_PANTS));
  check(wishes.stream().anyMatch(w->{try{return (Integer)invoke(w,"getItemId")==BRONZE_GAITERS;}catch(Exception e){return false;}}),"fighter kit legs are Bronze Gaiters");
  int alt=(Integer)invoke(singleton("population.economy.Market"),"bestAlternative",BRONZE_BREASTPLATE,40000);
  check(alt>0&&kind(ItemTable.getInstance().getTemplate(alt))==ArmorType.HEAVY,"cheaper stand-in for a heavy chest is heavy");

  System.out.println("ARMOR KIND ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
