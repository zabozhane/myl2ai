import java.util.*;
import kotlin.Pair;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.player.*;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.actors.*;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Controlled inventory/class/level/wallet fixtures; one normal core bow shot verifies the retained weapon. */
public class InventoryBowReadinessVerify extends WeaponGradeSuppliesVerify {
 static Monster prey;static Location field;
 static void field()throws Exception{
  List<Monster> candidates=new ArrayList<>();for(Object o:World.getInstance().getObjects())if(o instanceof Monster){Monster m=(Monster)o;if(!m.isDead()&&m.getLevel()<=4&&m.getName().contains("Keltir")&&!m.isInsideZone(ZoneId.TOWN)&&!m.isInsideZone(ZoneId.PEACE))candidates.add(m);}
  candidates.sort(Comparator.comparingDouble(m->Math.hypot(m.getX()+91300,m.getY()-240000)));GeoEngine geo=GeoEngine.getInstance();
  for(Monster m:candidates){int x=m.getX()-250,y=m.getY();if(!geo.hasGeo(x,y))continue;Location at=new Location(x,y,geo.getHeight(x,y,m.getZ()));if(!geo.canMoveToTarget(x,y,at.getZ(),m.getX(),m.getY(),m.getZ())||Boolean.TRUE.equals(invoke(singleton("population.RouteSafety"),"isWater",at)))continue;prey=m;field=at;return;}throw new AssertionError("No real accessible field prey");
 }
 static void equipment(Player p,boolean population)throws Exception{if(population)invoke(gear,"equipUpgradesFromInventory",p);else invoke(singleton("offlinelife.OfflineEquipment"),"equip",p);}
 static boolean contains(List<?> items,ItemInstance wanted){for(Object o:items)if(((ItemInstance)o).getObjectId()==wanted.getObjectId())return true;return false;}
 static void cases(Player p,boolean population,boolean shoot,boolean oldOnly)throws Exception{
  setup(p);ItemInstance desired=p.getInventory().addItem("Loot readiness fixture",279,1,p,null),other=p.getInventory().addItem("Loot readiness fixture",274,1,p,null);money(p,0);warp(p,field);check(!p.isInsideZone(ZoneId.TOWN)&&!p.isInsideZone(ZoneId.PEACE),"readiness fixture is outside town and peace protection");equipment(p,population);check(p.getActiveWeaponItem().getItemId()==13,"automatic inventory upgrade keeps a working bow when the new grade has no arrows");
  if(oldOnly){System.out.println("INVENTORY BOW READINESS PLAN ALL PASSED");System.exit(0);}
  if(shoot){Object combat=invoke(p,"getCombatBehavior");p.setTarget(prey);invoke(combat,"setCommittedTarget",prey);long arrows=p.getInventory().getInventoryItemCount(17,-1);double hp=prey.getCurrentHp();long until=System.currentTimeMillis()+30000;while(System.currentTimeMillis()<until&&(p.getInventory().getInventoryItemCount(17,-1)>=arrows||prey.getCurrentHp()>=hp)){await(combat,"onUpdate");Thread.sleep(500);}check(p.getInventory().getInventoryItemCount(17,-1)<arrows&&prey.getCurrentHp()<hp,"retained bow really consumes its owned arrow and damages a real monster");p.abortAttack();p.abortCast();p.setTarget(null);invoke(combat,"setCommittedTarget",(Object)null);p.getAI().setIntention(IntentionType.IDLE);long stopped=System.currentTimeMillis()+5000;while((p.isAttackingNow()||p.isCastingNow())&&System.currentTimeMillis()<stopped)Thread.sleep(100);}
  p.getInventory().addItem("Readiness stock fixture",1341,1500,p,null);equipment(p,population);check(p.getActiveWeaponItem().getItemId()==279,"owned matching arrows allow the useful field upgrade");
  if(!population){ItemInstance shield=p.getInventory().addItem("Offhand protection fixture",19,1,p,null);invoke(gear,"equip",p,shield);equipment(p,false);check(shield.isEquipped()&&!desired.isEquipped(),"inventory upgrades preserve an actual equipped shield");}
  ItemInstance old=p.getInventory().getItemByItemId(13);invoke(gear,"equip",p,old);ItemInstance ngArrows=p.getInventory().getItemByItemId(17);if(!ngArrows.isEquipped())invoke(gear,"equip",p,ngArrows);check(ngArrows.isEquipped(),"town upgrade starts with actual equipped arrows");erase(p,1341);beside(p,npcId(30084));money(p,0);equipment(p,population);check(p.getActiveWeaponItem().getItemId()==13,"empty town wallet cannot promise ammunition for a new bow");List<?> junk=(List<?>)invoke(population?gear:singleton("offlinelife.OfflineEquipment"),"surplus",p);check(!contains(junk,desired)&&!contains(junk,other),"deferred stronger bows remain owned instead of being sold as junk");
  money(p,1000000);equipment(p,population);check(p.getActiveWeaponItem().getItemId()==279,"funded town resupply allows equipping the already owned upgrade");check(p.getInventory().getInventoryItemCount(279,-1)==1,"an owned bow upgrade needs no duplicate purchase");check(!p.isGM()&&!p.isInvul(),"readiness uses ordinary access and no invulnerability");System.out.println("INVENTORY BOW INFO population="+population+" arrows="+p.getInventory().getInventoryItemCount(17,-1)+" cash="+p.getAdena());
 }
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager"),manager=singleton("AutobotsManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");gear=singleton("population.economy.Gear");market=singleton("population.economy.Market");offline=singleton("offlinelife.OfflineChores");field();
  Autobot b=new Autobot(IdFactory.getInstance().getNextId(),PlayerData.getInstance().getTemplate(ClassId.ROGUE),new Appearance((byte)0,(byte)0,(byte)0,Sex.MALE));b.setName("InventoryBowPolicy");b.setAccessLevel(0);b.setXYZ(npcId(30084).getX()+40,npcId(30084).getY()+40,npcId(30084).getZ());b.getStat().setLevel((byte)39);b.getStat().setExp(Experience.LEVEL[39]);suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);invoke(b,"setLife",Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b));cases(b,true,true,Arrays.asList(args).contains("--plan-only"));
  Player p=Player.restore(268486152);check(p!=null,"offline owner restores for inventory readiness");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);p.setClassId(ClassId.ROGUE.getId());p.getStat().setLevel((byte)39);p.getStat().setExp(Experience.LEVEL[39]);invoke(AutofarmManager.INSTANCE,"prepareOfflineBehavior",p);cases(p,false,false,false);System.out.println("INVENTORY BOW READINESS ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
