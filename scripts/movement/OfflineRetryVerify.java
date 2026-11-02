import java.util.*;
import kotlin.Pair;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Failed route memory survives mere activation; real XP and finite expiry release it. */
public class OfflineRetryVerify extends OfflineTownVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object j:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(j,"cancel");
  Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"snapshot restores offline farmer");
  AutofarmManager.INSTANCE.stopFarm(p,"retry verification");warp(p,new Location(-63348,110980,-3600));
  p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
  @SuppressWarnings("unchecked") Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);farmers.entrySet().removeIf(e->e.getKey()!=p.getObjectId());
  for(String field:new String[]{"traveling","huntProgress","retryAfter","idleSince"})((Map<?,?>)member(m,field).get(m)).clear();
  for(String field:new String[]{"lessonCheckedAt","townCheckedAt"})stamp(m,field,p,System.currentTimeMillis()+3600000);
  @SuppressWarnings("unchecked") Map<Integer,Set<Long>> failed=(Map<Integer,Set<Long>>)member(m,"unreachableSpots").get(m);
  @SuppressWarnings("unchecked") Map<Integer,Long> rounds=(Map<Integer,Long>)member(m,"failedRoundAt").get(m);
  failed.put(p.getObjectId(),new HashSet<>(Arrays.asList(123L,456L)));rounds.put(p.getObjectId(),System.currentTimeMillis()-10000);
  invoke(m,"startFarmLogged",p,"test activation");check(AutofarmManager.INSTANCE.isAutoFarming(p),"fixture can enable hunting");
  check(failed.containsKey(p.getObjectId())&&failed.get(p.getObjectId()).size()==2&&rounds.containsKey(p.getObjectId()),"mere activation preserves failed targets and search-round age");
  AutofarmManager.INSTANCE.stopFarm(p,"test idle recovery");stamp(m,"idleSince",p,System.currentTimeMillis()-31000);invoke(m,"tick");
  check(AutofarmManager.INSTANCE.isAutoFarming(p)&&failed.containsKey(p.getObjectId())&&failed.get(p.getObjectId()).size()==2,"idle recovery cannot erase failures before a fight");
  @SuppressWarnings("unchecked") Map<Integer,Pair<Long,Long>> progress=(Map<Integer,Pair<Long,Long>>)member(m,"huntProgress").get(m);
  long before=p.getExp();progress.put(p.getObjectId(),new Pair<>(before,System.currentTimeMillis()));
  long deadline=System.currentTimeMillis()+120000;
  while(p.getExp()<=before&&!p.isDead()&&System.currentTimeMillis()<deadline){suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);invoke(m,"tick");Thread.sleep(500);}
  check(p.getExp()>before,"ordinary nearby combat earns real XP without GM protection");invoke(m,"tick");
  check(!failed.containsKey(p.getObjectId())&&!rounds.containsKey(p.getObjectId()),"real hunting progress releases previous failed-route memory");
  AutofarmManager.INSTANCE.stopFarm(p,"test finite expiry");p.abortAttack();p.abortCast();p.setTarget(null);p.stopMove(null);
  failed.put(p.getObjectId(),new HashSet<>(Arrays.asList(789L)));rounds.put(p.getObjectId(),System.currentTimeMillis()-301000);
  stamp(m,"idleSince",p,System.currentTimeMillis()-31000);invoke(m,"tick");
  check(!failed.containsKey(p.getObjectId())&&!rounds.containsKey(p.getObjectId()),"expired search round can retry without waiting five real minutes");
  System.out.println("OFFLINE RETRY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
