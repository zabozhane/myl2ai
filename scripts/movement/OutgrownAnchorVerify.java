import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Live: owner logged Zenthen (lv 20) out at Kaboo grunts (7-12); ordinary offline ticks must leave this outgrown anchor. */
public class OutgrownAnchorVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);
  System.out.println("OUTGROWN INFO at="+location(p)+" level="+p.getLevel()+" targetLevels="+invoke(singleton("population.HuntingSpots"),"targetLevels",p));
  AutofarmManager.INSTANCE.startFarm(p);Map<?,?> travels=(Map<?,?>)member(m,"traveling").get(m);long began=System.currentTimeMillis();boolean left=false;
  while(System.currentTimeMillis()-began<150000){invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
   if(travels.containsKey(p.getObjectId())||!AutofarmManager.INSTANCE.isAutoFarming(p)&&!p.isDead()){left=true;break;}Thread.sleep(300);}
  System.out.println("OUTGROWN RESULT seconds="+(System.currentTimeMillis()-began)/1000+" left="+left+" trip="+travels.get(p.getObjectId()));
  check(left,"Zenthen leaves the Kaboo grunt anchor for prey of its own level");
  System.out.println("OUTGROWN ANCHOR ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
