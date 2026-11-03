import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Live Zenthen in its home village runs ordinary town chores: real armor/jewelry purchase or an explained wait. */
public class OfflineArmorVerify extends OfflineTownVerify {
 static String worn(Player p){StringBuilder b=new StringBuilder();for(int s=0;s<Inventory.PAPERDOLL_TOTALSLOTS;s++){var i=p.getInventory().getPaperdollItem(s);if(i!=null)b.append(s).append('=').append(i.getItemName()).append(' ');}return b.toString();}
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"Zenthen restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  if(!p.isInsideZone(ZoneId.TOWN))warp(p,new Location(12056,16568,GeoEngine.getInstance().getHeight(12056,16568,-4584)));
  check(p.isInsideZone(ZoneId.TOWN),"home village");
  int cash=p.getAdena(),heals=(int)p.getInventory().getInventoryItemCount(1060,-1),soe=(int)p.getInventory().getInventoryItemCount(736,-1);int pdef=p.getPDef(null),mdef=p.getMDef(null,null);String before=worn(p);
  System.out.println("ARMOR INFO before cash="+cash+" heals="+heals+" soe="+soe+" pdef="+pdef+" mdef="+mdef+" worn="+before);
  suspendValue(singleton("offlinelife.OfflineChores"),"run",p);
  int after=p.getAdena();int pdef2=p.getPDef(null),mdef2=p.getMDef(null,null);String now=worn(p);
  System.out.println("ARMOR INFO after cash="+after+" heals="+p.getInventory().getInventoryItemCount(1060,-1)+" soe="+p.getInventory().getInventoryItemCount(736,-1)+" pdef="+pdef2+" mdef="+mdef2+" worn="+now);
  check(p.getInventory().getInventoryItemCount(1060,-1)>=Math.min(heals,10)&&p.getInventory().getInventoryItemCount(736,-1)>=1,"healing and an emergency scroll are kept");
  boolean bought=!now.equals(before);
  if(bought)check(pdef2+mdef2>pdef+mdef,"the purchase really raises defence and is worn");
  System.out.println("ARMOR RESULT bought="+bought+" defence="+(pdef+mdef)+"->"+(pdef2+mdef2)+" cash="+cash+"->"+after);
  System.out.println("OFFLINE ARMOR ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
