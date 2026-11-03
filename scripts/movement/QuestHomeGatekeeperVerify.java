import java.util.*;
import java.util.concurrent.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.player.*;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.data.xml.*;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.*;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Controlled eligibility/cash/stock; actual SoE, paid NPC teleport, final walk and quest-script continuation. */
public class QuestHomeGatekeeperVerify extends WeaponGradeSuppliesVerify {
 static Object gk;static Location origin;
 static void quietField(Player p)throws Exception {
  GeoEngine geo=GeoEngine.getInstance();List<Monster> mobs=new ArrayList<>();for(Object o:World.getInstance().getObjects())if(o instanceof Monster&&!((Monster)o).isDead())mobs.add((Monster)o);
  for(int r=0;r<=6000;r+=300)for(int i=0;i<16;i++){double a=2*Math.PI*i/16;int x=-28856+(int)(r*Math.cos(a)),y=133144+(int)(r*Math.sin(a));if(!geo.hasGeo(x,y))continue;int z=geo.getHeight(x,y,-3664);if(Math.abs(z+3664)>600||mobs.stream().anyMatch(m->Math.hypot(m.getX()-x,m.getY()-y)<650))continue;Location at=new Location(x,y,z);if(Boolean.TRUE.equals(invoke(singleton("population.RouteSafety"),"isWater",at)))continue;warp(p,at);if(p.isInsideZone(ZoneId.TOWN)||p.isInsideZone(ZoneId.PEACE))continue;Location town=MapRegionData.getInstance().getLocationToTeleport(p,MapRegionData.TeleportType.TOWN);if(gap(town,new Location(-14704,122032,-3000))>5000)continue;if(!((List<?>)invoke(gk,"offers",p)).isEmpty())continue;origin=at;System.out.println("QUEST HOME INFO field="+at+" home="+town);return;}throw new AssertionError("No quiet real Gludio field origin");
 }
 static Object prepare(Player p)throws Exception {
  p.setClassId(ClassId.ORC_FIGHTER.getId());p.setBaseClass(ClassId.ORC_FIGHTER);p.getStat().setLevel((byte)19);p.getStat().setExp(Experience.LEVEL[19]+1000);p.setAccessLevel(0);p.setIsInvul(false);p.setTarget(null);for(int id:new int[]{736,1060,1061})erase(p,id);p.getInventory().addItem("Quest route supply fixture",736,2,p,null);p.getInventory().addItem("Quest route supply fixture",1060,10,p,null);money(p,100000);invoke(singleton("population.quests.QuestRetryStore"),"clear",p.getObjectId());quietField(p);
  Object r=Class.forName("dev.l2j.autobots.population.quests.QuestRunner").getConstructor(Player.class).newInstance(p);List<?> paths=(List<?>)invoke(singleton("population.quests.PathQuests"),"forBaseClass",ClassId.ORC_FIGHTER);Object path=paths.stream().filter(q->{try{return "Q414_PathToAnOrcRaider".equals(invoke(q,"getQuestName"));}catch(Exception e){throw new RuntimeException(e);}}).findFirst().orElseThrow();member(r,"chosen").set(r,path);check(p.getQuestState("Q414_PathToAnOrcRaider")==null||!p.getQuestState("Q414_PathToAnOrcRaider").isStarted(),"controlled actor has not taken the target profession quest");return r;
 }
 static void waitAction(Player p,CompletableFuture<Object> f,long deadline)throws Exception{while(!f.isDone()&&System.currentTimeMillis()<deadline){if(p.isTeleporting())p.onTeleported();Thread.sleep(250);}check(f.isDone(),"quest action completes within a finite travel budget");f.get();if(p.isTeleporting())p.onTeleported();}
 static void cases(Player p,boolean oldOnly)throws Exception {
  Object r=prepare(p);Npc goal=npcId(30570);long scrolls=p.getInventory().getInventoryItemCount(736,-1);int cash=p.getAdena();
  if(oldOnly){CompletableFuture<Object> f=async(r,"travelTo",new Location(goal.getX(),goal.getY(),goal.getZ()),800);long end=System.currentTimeMillis()+8000;while(System.currentTimeMillis()<end&&!p.isCastingNow()&&p.getInventory().getInventoryItemCount(736,-1)==scrolls)Thread.sleep(200);check(p.isCastingNow()||p.getInventory().getInventoryItemCount(736,-1)<scrolls,"field quest returns to town for its actual available gatekeeper");System.out.println("QUEST HOME GATEKEEPER PLAN ALL PASSED");System.exit(0);}
  Location town=MapRegionData.getInstance().getLocationToTeleport(p,MapRegionData.TeleportType.TOWN);Object quote=invoke(gk,"towardFrom",p,town,new Location(goal.getX(),goal.getY(),goal.getZ()),2000.0);check(quote!=null&&((Npc)invoke(quote,"getGatekeeper")).getNpcId()==30256,"actual home gatekeeper quotes the real Orc Village service");int fare=(Integer)invoke(quote,"getPrice");money(p,fare);waitAction(p,async(r,"travelTo",new Location(goal.getX(),goal.getY(),goal.getZ()),800),System.currentTimeMillis()+8000);check(!p.isCastingNow()&&p.getInventory().getInventoryItemCount(736,-1)==scrolls&&!p.isInsideZone(ZoneId.TOWN),"home fare also preserves the reserve after consuming SoE");money(p,cash);long start=System.currentTimeMillis(),deadline=start+260000;
  while(System.currentTimeMillis()<deadline&&!p.isDead()&&(p.getQuestState("Q414_PathToAnOrcRaider")==null||!p.getQuestState("Q414_PathToAnOrcRaider").isStarted())){waitAction(p,async(r,"onUpdate"),deadline);if(!((Boolean)invoke(r,"professionBusiness")))throw new AssertionError("Quest route was deferred; inspect actual movement failure");Thread.sleep(300);}
  check(!p.isDead()&&p.getQuestState("Q414_PathToAnOrcRaider")!=null&&p.getQuestState("Q414_PathToAnOrcRaider").isStarted(),"quest reaches its actual NPC and the normal quest script starts");check(goal.isInsideRadius(p,Npc.INTERACTION_DISTANCE,true,false),"actor physically arrives within NPC interaction range");check(p.getInventory().getInventoryItemCount(736,-1)==scrolls-1&&cash-p.getAdena()==fare,"route consumes its own SoE and pays exactly the actual quoted NPC fare");check(p.getClassId()==ClassId.ORC_FIGHTER&&p.getInventory().getInventoryItemCount(1592,-1)==0&&!p.isGM()&&!p.isInvul(),"route grants no free profession, mark or GM protection");System.out.println("QUEST HOME INFO actor="+p.getName()+" fare="+fare+" seconds="+(System.currentTimeMillis()-start)/1000+" at="+location(p));
 }
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");gear=singleton("population.economy.Gear");gk=singleton("population.economy.GatekeeperTravel");
  Player p=Player.restore(268486152);p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);cases(p,Arrays.asList(args).contains("--plan-only"));
  Object manager=singleton("AutobotsManager");Autobot b=new Autobot(IdFactory.getInstance().getNextId(),PlayerData.getInstance().getTemplate(ClassId.ORC_FIGHTER),new Appearance((byte)0,(byte)0,(byte)0,Sex.MALE));b.setName("QuestTownOrigin");b.setAccessLevel(0);b.setXYZ(-14704,122032,-3000);b.getStat().setLevel((byte)19);b.getStat().setExp(Experience.LEVEL[19]+1000);suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);invoke(b,"setLife",Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b));cases(b,false);System.out.println("QUEST HOME GATEKEEPER ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
