import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Live offline Orc Mystic Afrodita (lv 13, own SP/adena): buys its spellbooks in town and learns Life Drain at a trainer. */
public class MageSpellbookVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268480957);check(p!=null,"Afrodita restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  check(p.getSkill(1090)==null,"Life Drain is not known yet");
  warp(p,MapRegionData.getInstance().getLocationToTeleport(p,MapRegionData.TeleportType.TOWN));
  int cash=p.getAdena(),sp=p.getSp();System.out.println("MAGE BOOK INFO cash="+cash+" sp="+sp+" at="+location(p));
  suspendValue(m,"choresThenBack",p,null);
  System.out.println("MAGE BOOK INFO after chores cash="+p.getAdena()+" lifeDrainBook="+p.getInventory().getInventoryItemCount(1525,-1));
  check(p.getInventory().getInventoryItemCount(1525,-1)>0,"the Life Drain book is bought with own money");
  ((Map<Integer,Long>)member(m,"lessonCheckedAt").get(m)).remove(p.getObjectId());((Map<Integer,Long>)member(m,"retryAfter").get(m)).remove(p.getObjectId());
  long began=System.currentTimeMillis();
  while(System.currentTimeMillis()-began<300000&&p.getSkill(1090)==null&&!p.isDead()){invoke(m,"tick");Thread.sleep(300);}
  System.out.println("MAGE BOOK RESULT seconds="+(System.currentTimeMillis()-began)/1000+" lifeDrain="+(p.getSkill(1090)!=null)+" sp="+sp+"->"+p.getSp()+" book="+p.getInventory().getInventoryItemCount(1525,-1)+" skills="+p.getSkills().keySet());
  check(p.getSkill(1090)!=null&&p.getSp()<sp,"Life Drain is learned at a trainer for SP");
  System.out.println("MAGE SPELLBOOK ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
