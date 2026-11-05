import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import dev.l2j.autobots.autofarm.AutofarmManager;

/**
 * Offline HawkEye (Knight 22) in its home town. Phase 1, real wallet: no local stand-in is bought while the guide's
 * D-grade piece is sold elsewhere. Phase 2, CONTROLLED wallet on the stand only (adena set to 300000, never on the
 * main server): one real gatekeeper ride to the selling town, purchase and wearing of a guide piece.
 */
public class GuideTripVerify extends OfflineTownVerify {
 static String worn(Player p){StringBuilder b=new StringBuilder();for(int s=0;s<Inventory.PAPERDOLL_TOTALSLOTS;s++){var i=p.getInventory().getPaperdollItem(s);if(i!=null)b.append(s).append('=').append(i.getItemName()).append(' ');}return b.toString();}
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268491948);p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  warp(p,MapRegionData.getInstance().getLocationToTeleport(p,MapRegionData.TeleportType.TOWN));
  String before=worn(p);int cash=p.getAdena();
  suspendValue(singleton("offlinelife.OfflineChores"),"run",p);
  System.out.println("GUIDE TRIP INFO phase1 cash="+cash+"->"+p.getAdena()+" worn changed="+!before.equals(worn(p))+" at="+location(p));
  check(before.equals(worn(p)),"with a small wallet no stand-in is bought while saving for the guide piece");
  warp(p,MapRegionData.getInstance().getLocationToTeleport(p,MapRegionData.TeleportType.TOWN));
  p.reduceAdena("Verify",p.getAdena(),null,false);p.addAdena("Verify controlled stand wallet",300000,null,false);
  Object home=location(p);String before2=worn(p);
  suspendValue(singleton("offlinelife.OfflineChores"),"run",p);
  System.out.println("GUIDE TRIP INFO phase2 cash=300000->"+p.getAdena()+" from="+home+" at="+location(p)+" worn: "+worn(p));
  check(!before2.equals(worn(p))&&Math.hypot(p.getX()-((net.sf.l2j.gameserver.model.location.Location)home).getX(),p.getY()-((net.sf.l2j.gameserver.model.location.Location)home).getY())>5000,"a real ride to another town buys and wears a guide piece");
  System.out.println("GUIDE TRIP ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
