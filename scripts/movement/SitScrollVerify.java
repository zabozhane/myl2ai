import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** A resting Zenthen with its own scrolls starts a healing trip: the scroll must really take it to town. */
public class SitScrollVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"Zenthen restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  int scrolls=(int)p.getInventory().getInventoryItemCount(736,-1);check(scrolls>0,"live snapshot owns its scrolls of escape");
  if(p.isInsideZone(ZoneId.TOWN))warp(p,new Location(-29192,50584,GeoEngine.getInstance().getHeight(-29192,50584,-3624)));
  check(!p.isInsideZone(ZoneId.TOWN),"field origin");
  p.sitDown();long sitBy=System.currentTimeMillis()+6000;while(!p.isSitting()&&System.currentTimeMillis()<sitBy)Thread.sleep(100);Thread.sleep(3000);check(p.isSitting(),"resting like the live character when the trip starts");
  long began=System.currentTimeMillis();invoke(m,"startTownTrip",p,true,"healing_stock");
  long until=began+60000;while(System.currentTimeMillis()<until&&!p.isInsideZone(ZoneId.TOWN)){if(p.isTeleporting())p.onTeleported();Thread.sleep(200);}
  int after=(int)p.getInventory().getInventoryItemCount(736,-1);
  System.out.println("SIT SCROLL RESULT seconds="+(System.currentTimeMillis()-began)/1000+" town="+p.isInsideZone(ZoneId.TOWN)+" at="+location(p)+" scrolls="+scrolls+"->"+after);
  check(p.isInsideZone(ZoneId.TOWN)&&after==scrolls-1,"the owned scroll actually takes the resting character to town");
  check(!p.isGM()&&!p.isInvul(),"no GM or invulnerability");
  System.out.println("SIT SCROLL ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
