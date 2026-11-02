import java.util.*;
import java.util.concurrent.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Failed batches must advance; unavailable planner slots must not blacklist untouched spots. */
public class HuntCandidateHistoryVerify extends TownUnstuckVerify {
 static void retryNow(Object life)throws Exception{member(life,"nextSpotRetryAt").setLong(life,0);member(life,"nextRecoveryRetryAt").setLong(life,0);}
 @SuppressWarnings({"unchecked","rawtypes"}) public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");Object manager=singleton("AutobotsManager");Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Mapgyk");check(b!=null&&b.getLevel()==13,"recorded stalled caster restores");
  suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);b.abortAttack();b.abortCast();b.stopMove(null);b.setTarget(null);b.setAccessLevel(0);b.setIsInvul(false);warp(b,new Location(22072,18648,-3536));
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);Object session=member(life,"session").get(life);invoke(session,"switchTo",Enum.valueOf((Class)Class.forName("dev.l2j.autobots.population.SessionPlan$Activity"),"Hunt"),30,"recorded search history fixture");Map<Long,Long> bad=(Map<Long,Long>)member(life,"badSpots").get(life);
  Semaphore permits=(Semaphore)member(singleton("offlinelife.HuntRecovery"),"walkingSearches").get(singleton("offlinelife.HuntRecovery"));check(permits.tryAcquire(2),"isolated fixture holds both available planner slots");try{suspendValue(life,"hunt");check(bad.isEmpty(),"busy planner slots do not reject unexamined candidates");}finally{permits.release(2);}
  retryNow(life);Object target=invoke(life,"currentSpot");check(!bad.isEmpty(),"failed actual route checks survive into the next selection attempt");
  if(Arrays.asList(args).contains("--history-only")){System.out.println("HUNT CANDIDATE HISTORY PLAN ALL PASSED");System.exit(0);}
  int rounds=1;while(target==null&&rounds<8){retryNow(life);target=invoke(life,"currentSpot");rounds++;System.out.println("HUNT HISTORY INFO round="+rounds+" failed="+bad.size()+" selected="+target);}
  check(target!=null&&bad.size()>8,"later search batches reach a valid goal beyond the original nearest eight");Object recovery=member(life,"recovery").get(life);check(recovery!=null,"actual population selector retains its validated recovery");check(invoke(life,"currentSpot")==target,"next update keeps the new reachable goal");Object walk=invoke(recovery,"getWalk");Location origin=location(b),goal=(Location)invoke(recovery,"getAnchor");routeValid(b,origin,walk,"later candidate recovery");long scrolls=b.getInventory().getInventoryItemCount(736,-1),began=System.currentTimeMillis();
  CompletableFuture<Object> travel=async(life,"travelTo",target,false);long deadline=began+840000,reported=began;
  while(!travel.isDone()&&System.currentTimeMillis()<deadline){if(b.isDead())throw new AssertionError("caster died during later-candidate recovery");if(System.currentTimeMillis()-reported>=20000){reported=System.currentTimeMillis();System.out.println("HUNT HISTORY INFO seconds="+(reported-began)/1000+" at="+location(b)+" hp="+(int)b.getCurrentHp()+" goal="+goal);}Thread.sleep(500);}
  check(travel.isDone(),"later candidate walk finishes inside the original finite journey budget");travel.get();check(!b.isDead()&&gap(location(b),goal)<=650,"caster physically arrives at its later reachable prey");check(b.getInventory().getInventoryItemCount(736,-1)==scrolls,"walking recovery consumes no escape scroll");long xp=b.getExp();deadline=System.currentTimeMillis()+180000;
  while(b.getExp()<=xp&&!b.isDead()&&System.currentTimeMillis()<deadline){suspendValue(invoke(b,"getCombatBehavior"),"onUpdate");Thread.sleep(500);}
  check(b.getExp()>xp,"ordinary spell combat gains experience after the new arrival");check(!b.isGM()&&!b.isInvul(),"history recovery uses ordinary combat rules");System.out.println("HUNT HISTORY INFO xp="+xp+"->"+b.getExp()+" failed="+bad.size()+" rounds="+rounds+" seconds="+(System.currentTimeMillis()-began)/1000);System.out.println("HUNT CANDIDATE HISTORY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
