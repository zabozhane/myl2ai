import java.util.*;
import kotlin.Pair;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Decision/dialog fixture: positions and timer ages are controlled; quest items/reward are real. */
public class QuestTownDestinationVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") static void readyCheck(Object m,Player p)throws Exception{
  long now=System.currentTimeMillis();stamp(m,"lastTownTripAt",p,now-3600000);stamp(m,"productiveSince",p,now-660000);stamp(m,"townCheckedAt",p,0);stamp(m,"lessonCheckedAt",p,now+3600000);stamp(m,"equipmentCheckedAt",p,now+3600000);((Map<?,?>)member(m,"townRetryAfter").get(m)).remove(p.getObjectId());((Map<Integer,Pair<Long,Long>>)member(m,"huntProgress").get(m)).put(p.getObjectId(),new Pair<>(p.getExp(),now));invoke(m,"tick");
 }
 static long stones(Player p){return p.getInventory().getInventoryItemCount(1475,-1)+p.getInventory().getInventoryItemCount(1476,-1);}
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"real quest actor restores");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();p.setAccessLevel(0);p.setIsInvul(false);((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(m,"ensureQuestsLoaded",p);
  check(p.getQuestState("Q273_InvadersOfTheHolyLand").isStarted()&&stones(p)==48,"recorded quest is genuinely ready with 48 owned stones");Npc giver=npcId(30566);
  warp(p,new Location(-23689,142316,-3768));Location home=MapRegionData.getInstance().getLocationToTeleport(p,MapRegionData.TeleportType.TOWN);System.out.println("QUEST TOWN INFO home="+home+" giver="+giver.getName()+" distance="+(int)gap(home,new Location(giver.getX(),giver.getY(),giver.getZ())));invoke(m,"startFarmLogged",p,"quest destination fixture");readyCheck(m,p);check(!hasTownAction(m,p)&&AutofarmManager.INSTANCE.isAutoFarming(p),"actual offline tick keeps hunting when the return town cannot serve the ready quest");
  if(args.length>0&&args[0].equals("--foreign-only")){System.out.println("QUEST TOWN FOREIGN ALL PASSED");System.exit(0);}
  check(Boolean.FALSE.equals(invoke(m,"repeatQuestReadyAtHome",p)),"remote quest giver is not a reason for this town trip");invoke(AutofarmManager.INSTANCE,"quiesce",p);warp(p,new Location(-40824,-109208,-1808));check(Boolean.TRUE.equals(invoke(m,"repeatQuestReadyAtHome",p)),"home region correctly recognizes the ready local giver");
  Object settings=invoke(singleton("AutobotData"),"getSettings");Object enabled=invoke(settings,"getQuestsEnabled");invoke(settings,"setQuestsEnabled",false);try{check(Boolean.FALSE.equals(invoke(m,"repeatQuestReadyAtHome",p)),"disabled quests cannot cause pointless offline town trips");}finally{invoke(settings,"setQuestsEnabled",enabled);}
  invoke(m,"startFarmLogged",p,"correct quest home fixture");readyCheck(m,p);check(hasTownAction(m,p),"actual offline tick still schedules a useful local quest return");cancelTown(m,p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  beside(p,giver);long before=p.getAdena(),items=stones(p);suspendValue(singleton("offlinelife.OfflineChores"),"turnInQuests",p);check(items==48&&stones(p)==0&&p.getAdena()>before,"normal NPC dialog consumes real quest loot and pays its actual reward");check(Boolean.FALSE.equals(invoke(m,"repeatQuestReadyAtHome",p)),"completed turn-in cannot immediately request another trip");check(!p.isGM()&&!p.isInvul(),"decision/dialog fixture uses ordinary game rules");System.out.println("QUEST TOWN INFO reward="+(p.getAdena()-before)+" cash="+p.getAdena());System.out.println("QUEST TOWN DESTINATION ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
