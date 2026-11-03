import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.actor.player.*;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.*;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Owned alternate bow/stock, real combat tick and core arrow use; controlled class/level/inventory. */
public class BowAmmoFallbackVerify extends InventoryBowReadinessVerify {
 static Object combat(Player p)throws Exception{return p instanceof Autobot?invoke(p,"getCombatBehavior"):((Map<?,?>)member(AutofarmManager.INSTANCE,"combatBehaviors").get(AutofarmManager.INSTANCE)).get(p.getObjectId());}
 static void shot(Player p,int arrow)throws Exception {
  field();warp(p,field);Object combat=combat(p);p.setTarget(prey);invoke(combat,"setCommittedTarget",prey);long count=p.getInventory().getInventoryItemCount(arrow,-1);double hp=prey.getCurrentHp();long until=System.currentTimeMillis()+15000;
  while(System.currentTimeMillis()<until&&(p.getInventory().getInventoryItemCount(arrow,-1)>=count||prey.getCurrentHp()>=hp)){await(combat,"onUpdate");Thread.sleep(500);}
  check(p.getInventory().getInventoryItemCount(arrow,-1)<count&&prey.getCurrentHp()<hp,"ordinary core shot consumes arrow "+arrow+" and damages real prey");p.abortAttack();p.abortCast();p.getAI().setIntention(IntentionType.IDLE);p.setTarget(null);invoke(combat,"setCommittedTarget",(Object)null);long stopped=System.currentTimeMillis()+5000;while(p.isAttackingNow()&&System.currentTimeMillis()<stopped)Thread.sleep(100);
 }
 static void exhaustion(Player p)throws Exception {
  setup(p);ItemInstance bow=p.getInventory().addItem("Last arrow fixture",279,1,p,null);invoke(gear,"equip",p,bow);p.getInventory().addItem("Last arrow fixture",1341,1,p,null);money(p,0);if(p.isSitting()){p.standUp();Thread.sleep(3000);}shot(p,1341);check(p.getInventory().getInventoryItemCount(1341,-1)==0,"last matching arrow is exhausted by actual core combat");shot(p,17);check(p.getActiveWeaponItem().getItemId()==13,"combat continues on the owned fallback after actual exhaustion");
 }
 static void cases(Player p,boolean oldOnly)throws Exception {
  setup(p);ItemInstance empty=p.getInventory().addItem("Empty bow fixture",279,1,p,null);invoke(gear,"equip",p,empty);money(p,0);field();warp(p,field);Object combat=combat(p);p.setTarget(prey);invoke(combat,"setCommittedTarget",prey);
  long arrows=p.getInventory().getInventoryItemCount(17,-1);double hp=prey.getCurrentHp();long until=System.currentTimeMillis()+12000;
  while(System.currentTimeMillis()<until&&(p.getActiveWeaponItem().getItemId()!=13||p.getInventory().getInventoryItemCount(17,-1)>=arrows||prey.getCurrentHp()>=hp)){await(combat,"onUpdate");Thread.sleep(500);}
  check(p.getActiveWeaponItem().getItemId()==13,"combat replaces an empty bow with an owned usable bow");check(p.getInventory().getInventoryItemCount(17,-1)<arrows&&prey.getCurrentHp()<hp,"fallback continues ordinary combat with a real owned arrow");check(p.getInventory().getItemByObjectId(empty.getObjectId())!=null,"empty better bow stays owned for resupply");check(p.getAdena()==0&&!p.isGM()&&!p.isInvul(),"fallback needs no gold, item creation, GM or invulnerability");
  p.abortAttack();p.abortCast();p.getAI().setIntention(IntentionType.IDLE);p.setTarget(null);invoke(combat,"setCommittedTarget",(Object)null);long stopped=System.currentTimeMillis()+5000;while(p.isAttackingNow()&&System.currentTimeMillis()<stopped)Thread.sleep(100);
  if(oldOnly){System.out.println("BOW AMMO FALLBACK PLAN ALL PASSED");System.exit(0);}
  long stock=p.getInventory().getInventoryItemCount(17,-1);await(combat,"idleUpdate");check(p.getActiveWeaponItem().getItemId()==13&&p.getInventory().getInventoryItemCount(17,-1)==stock,"idle support does not oscillate to the empty better bow");
  erase(p,17);await(combat,"idleUpdate");check(p.getActiveWeaponItem().getItemId()==13,"no usable alternative leaves equipment and inventory intact");exhaustion(p);
 }
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager"),manager=singleton("AutobotsManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");gear=singleton("population.economy.Gear");market=singleton("population.economy.Market");offline=singleton("offlinelife.OfflineChores");
  Autobot b=new Autobot(IdFactory.getInstance().getNextId(),PlayerData.getInstance().getTemplate(ClassId.ROGUE),new Appearance((byte)0,(byte)0,(byte)0,Sex.MALE));b.setName("BowAmmoFallback");b.setAccessLevel(0);b.setXYZ(npcId(30084).getX()+40,npcId(30084).getY()+40,npcId(30084).getZ());b.getStat().setLevel((byte)39);b.getStat().setExp(Experience.LEVEL[39]);suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);invoke(b,"setLife",Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b));cases(b,Arrays.asList(args).contains("--plan-only"));
  Player p=Player.restore(268486152);check(p!=null,"offline owner restores for fallback");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);p.setClassId(ClassId.ROGUE.getId());p.getStat().setLevel((byte)39);p.getStat().setExp(Experience.LEVEL[39]);invoke(AutofarmManager.INSTANCE,"prepareOfflineBehavior",p);cases(p,false);System.out.println("BOW AMMO FALLBACK ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
