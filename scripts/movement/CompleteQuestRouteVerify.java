import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.actor.player.*;
import net.sf.l2j.gameserver.model.location.*;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.actors.*;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Controlled low budget reproduces the observed partial quest hop; actual funded NPC continuation. */
public class CompleteQuestRouteVerify extends WeaponGradeSuppliesVerify {
 static Location from=new Location(12102,17401,-4563);static Npc goal;static Object travel;
 static Object prepare(Player p)throws Exception{
  p.setClassId(ClassId.ORC_FIGHTER.getId());p.setBaseClass(ClassId.ORC_FIGHTER);p.getStat().setLevel((byte)20);p.getStat().setExp(Experience.LEVEL[20]+1000);p.setAccessLevel(0);p.setIsInvul(false);p.setTarget(null);for(int id:new int[]{1060,1061,1539,1540,736})erase(p,id);p.getInventory().addItem("Complete quest budget fixture",1060,10,p,null);p.getInventory().addItem("Complete quest budget fixture",736,2,p,null);money(p,4000);warp(p,from);invoke(singleton("population.quests.QuestRetryStore"),"clear",p.getObjectId());Object r=Class.forName("dev.l2j.autobots.population.quests.QuestRunner").getConstructor(Player.class).newInstance(p);for(Object path:(List<?>)invoke(singleton("population.quests.PathQuests"),"forBaseClass",ClassId.ORC_FIGHTER))if("Q415_PathToAMonk".equals(invoke(path,"getQuestName")))member(r,"chosen").set(r,path);return r;
 }
 static void low(Player p,Object r)throws Exception{
  Location target=new Location(goal.getX(),goal.getY(),goal.getZ());TeleportLocation exit=(TeleportLocation)invoke(r,"gatekeeperExitFor",target);check(exit==null||gap(exit,target)<=12000,"a cheap partial hop cannot stand in for a funded complete NPC route");
 }
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager"),manager=singleton("AutobotsManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");travel=singleton("population.economy.GatekeeperTravel");market=singleton("population.economy.Market");goal=npcId(30587);
  Player p=Player.restore(268486152);p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);Object r=prepare(p);low(p,r);
  if(Arrays.asList(args).contains("--plan-only")){System.out.println("COMPLETE QUEST ROUTE PLAN ALL PASSED");System.exit(0);}
  long scrolls=p.getInventory().getInventoryItemCount(736,-1);int cash=p.getAdena();await(r,"onUpdate");check(p.getAdena()==cash&&p.getInventory().getInventoryItemCount(736,-1)==scrolls&&gap(location(p),from)<100,"unfunded complete quest attempt spends no fare or SoE and starts no map-wide walk");money(p,100000);r=Class.forName("dev.l2j.autobots.population.quests.QuestRunner").getConstructor(Player.class).newInstance(p);for(Object path:(List<?>)invoke(singleton("population.quests.PathQuests"),"forBaseClass",ClassId.ORC_FIGHTER))if("Q415_PathToAMonk".equals(invoke(path,"getQuestName")))member(r,"chosen").set(r,path);Object complete=invoke(r,"completeExitFrom",location(p),new Location(goal.getX(),goal.getY(),goal.getZ()),goal);check(complete!=null,"funded NPC route has a real offered exit and checked final approach");int fare=(Integer)invoke(complete,"getPrice");long start=System.currentTimeMillis();await(r,"onUpdate");check(p.getQuestState("Q415_PathToAMonk")!=null&&p.getQuestState("Q415_PathToAMonk").isStarted()&&goal.isInsideRadius(p,Npc.INTERACTION_DISTANCE,true,false),"funded owner physically reaches the NPC and starts the normal quest");check(100000-p.getAdena()==fare&&p.getInventory().getInventoryItemCount(736,-1)==scrolls,"funded town route pays exactly the complete ride and wastes no SoE");check(p.getClassId()==ClassId.ORC_FIGHTER&&p.getInventory().getInventoryItemCount(1615,-1)==0&&!p.isGM()&&!p.isInvul(),"route creates no profession, mark or protection");System.out.println("COMPLETE QUEST INFO fare="+fare+" seconds="+(System.currentTimeMillis()-start)/1000);
  Autobot b=new Autobot(IdFactory.getInstance().getNextId(),PlayerData.getInstance().getTemplate(ClassId.ORC_FIGHTER),new Appearance((byte)0,(byte)0,(byte)0,Sex.MALE));b.setName("CompleteQuest");b.setAccessLevel(0);b.setXYZ(from.getX(),from.getY(),from.getZ());b.getStat().setLevel((byte)20);b.getStat().setExp(Experience.LEVEL[20]+1000);suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);Object br=prepare(b);low(b,br);money(b,100000);check(invoke(br,"completeExitFrom",location(b),new Location(goal.getX(),goal.getY(),goal.getZ()),goal)!=null,"population also distinguishes the complete funded route from its cheap partial hop");System.out.println("COMPLETE QUEST ROUTE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
