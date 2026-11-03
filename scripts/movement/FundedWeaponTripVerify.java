import java.util.*;
import java.util.concurrent.Semaphore;
import kotlin.Pair;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Decision fixture: controlled field position, wallet and elapsed times; route checks are real. */
public class FundedWeaponTripVerify extends OfflineTownVerify {
 static void window(Object m,Player p)throws Exception{long now=System.currentTimeMillis();stamp(m,"lastTownTripAt",p,now-1200000);stamp(m,"productiveSince",p,now-660000);stamp(m,"townCheckedAt",p,0);stamp(m,"lessonCheckedAt",p,now+3600000);stamp(m,"equipmentCheckedAt",p,now+3600000);((Map<?,?>)member(m,"townRetryAfter").get(m)).remove(p.getObjectId());((Map<Integer,Pair<Long,Long>>)member(m,"huntProgress").get(m)).put(p.getObjectId(),new Pair<>(p.getExp(),now));}
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null&&p.getLevel()==15,"recorded level-15 owner restores");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();p.setAccessLevel(0);p.setIsInvul(false);((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(m,"ensureQuestsLoaded",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);warp(p,new Location(22072,18648,-3536));p.setTarget(null);
  check(!p.isInsideZone(net.sf.l2j.gameserver.enums.ZoneId.TOWN),"policy starts outside the actual town");check(p.getInventory().getInventoryItemCount(736,-1)==1&&p.getInventory().getInventoryItemCount(1060,-1)>=3,"recorded supplies include one outward escape");money(p,100000);invoke(m,"startFarmLogged",p,"funded weapon trip fixture");window(m,p);
  if(!Arrays.asList(args).contains("--decision-only")){
   Object planner=singleton("offlinelife.OfflineWeaponTrip");Object plan=invoke(planner,"project",p);check(plan!=null,"real home NPC and checked walking return admit the funded novice goal");Location home=(Location)invoke(plan,"getHome");Object walk=invoke(plan,"getWalk");routeValid(p,home,walk,"weapon errand walking return");
   Object market=singleton("population.economy.Market");int price=(Integer)invoke(market,"priceWithTax",invoke(plan,"getShop"),invoke(plan,"getProduct"));Pair<?,?> escape=(Pair<?,?>)invoke(market,"findProductAt",736,home);int scroll=(Integer)invoke(market,"priceWithTax",escape.getFirst(),escape.getSecond());int reserve=(Integer)invoke(singleton("population.economy.Errands"),"reserve",p);check((Long)invoke(plan,"getSupplies")>=scroll,"projection funds replacement of the scroll consumed on departure");
   money(p,price+reserve+scroll-1);check(invoke(planner,"project",p)==null,"one adena short after the outward scroll cannot justify a gear commute");Map<Integer,Object> plans=(Map<Integer,Object>)member(planner,"plans").get(planner);Map<Integer,Long> retries=(Map<Integer,Long>)member(planner,"retryAt").get(planner);plans.put(p.getObjectId(),plan);retries.put(p.getObjectId(),System.currentTimeMillis()+60000);check(invoke(planner,"ready",p)==null,"a previously funded plan is rejected when current money no longer covers it");
   money(p,100000);retries.remove(p.getObjectId());Semaphore permits=(Semaphore)member(planner,"searches").get(planner);check(permits.tryAcquire(),"isolated fixture holds the sole background route-planning slot");try{window(m,p);long began=System.currentTimeMillis();invoke(m,"tick");check(System.currentTimeMillis()-began<2500&&!hasTownAction(m,p)&&AutofarmManager.INSTANCE.isAutoFarming(p),"busy optional planning leaves hunting active without blocking the farm tick");}finally{permits.release();}
  }
  long until=System.currentTimeMillis()+25000;while(!hasTownAction(m,p)&&System.currentTimeMillis()<until){window(m,p);invoke(m,"tick");Thread.sleep(300);}
  check(hasTownAction(m,p),"actual offline manager schedules a funded equipment errand after useful hunting");long castUntil=System.currentTimeMillis()+3000;while(!p.isCastingNow()&&System.currentTimeMillis()<castUntil)Thread.sleep(100);check(p.isCastingNow()&&!AutofarmManager.INSTANCE.isAutoFarming(p),"approved errand starts an ordinary own-scroll cast under the town action owner");cancelTown(m,p);check(!p.isGM()&&!p.isInvul(),"decision and planning use ordinary actor rules");System.out.println("FUNDED WEAPON TRIP ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
