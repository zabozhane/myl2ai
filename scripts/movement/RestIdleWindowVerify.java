import java.util.*;
import kotlin.Pair;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Inject idle age; useful sitting time must not expire a hunting anchor. */
public class RestIdleWindowVerify extends ScarceHealingVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"recorded actor restores for timer-only fixture");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);warp(p,new Location(-23689,142316,-3768));
  long now=System.currentTimeMillis();for(String field:new String[]{"townCheckedAt","lessonCheckedAt","equipmentCheckedAt"})times(m,field).put(p.getObjectId(),now+3600000);invoke(m,"startFarmLogged",p,"rest idle window verification");
  Map<Integer,Pair<Long,Long>> progress=(Map<Integer,Pair<Long,Long>>)member(m,"huntProgress").get(m);p.sitDown();check(p.isSitting(),"ordinary timer fixture actually sits");progress.put(p.getObjectId(),new Pair<>(p.getExp(),now-46000));invoke(m,"tick");check(progress.get(p.getObjectId()).getSecond()>=now,"rest refreshes the consecutive idle window without waiting 45 seconds");
  long animation=System.currentTimeMillis()+6000;while(p.isParalyzed()&&System.currentTimeMillis()<animation)Thread.sleep(20);check(!p.isParalyzed(),"normal sitting animation finishes");p.standUp();animation=System.currentTimeMillis()+6000;while(p.isSitting()&&System.currentTimeMillis()<animation)Thread.sleep(20);check(!p.isSitting(),"normal standing animation finishes");invoke(m,"tick");check(AutofarmManager.INSTANCE.isAutoFarming(p)&&!((Map<?,?>)member(m,"traveling").get(m)).containsKey(p.getObjectId())&&!((Map<?,?>)member(m,"townJobs").get(m)).containsKey(p.getObjectId()),"standing up gives combat a new chance on the same anchor");
  progress.put(p.getObjectId(),new Pair<>(p.getExp(),System.currentTimeMillis()-46000));invoke(m,"tick");check(progress.get(p.getObjectId())==null,"real awake idle still triggers the existing recovery");System.out.println("REST IDLE WINDOW ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
