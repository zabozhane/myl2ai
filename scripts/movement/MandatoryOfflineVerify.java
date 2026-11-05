import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.autofarm.AutofarmManager;

/**
 * save: Zenthen (auto hunt off, stored offlineEnabled=false) and a dead HawkEye log out: both stay online in offline
 * life; a character in a private store does not. resume (next JVM, same DB): the server restart brings Zenthen back.
 */
public class MandatoryOfflineVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") static Player online(Object m,int id)throws Exception{Player p=Player.restore(id);p.setAccessLevel(0);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();return p;}
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  boolean save=Arrays.asList(args).contains("save");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");
  Map<Integer,Player> farming=(Map<Integer,Player>)member(m,"farming").get(m);
  if(!save){Thread.sleep(8000);Player z=World.getInstance().getPlayer(268486152);
   System.out.println("MANDATORY INFO resume zenthenOnline="+(z!=null)+" farming="+farming.containsKey(268486152)+" offlineEnabled="+(z==null?"-":invoke(invoke(AutofarmManager.INSTANCE,"getSettings",z),"getOfflineEnabled")));
   check(z!=null&&farming.containsKey(268486152),"after a restart the stored offlineEnabled=false no longer leaves Zenthen offline");
   System.out.println("MANDATORY OFFLINE RESUME ALL PASSED");System.exit(0);}
  invoke(member(m,"travelTask").get(m),"cancel");
  Player z=online(m,268486152);invoke(invoke(AutofarmManager.INSTANCE,"getSettings",z),"setOfflineEnabled",false);invoke(AutofarmManager.INSTANCE,"onSettingsChanged",z);AutofarmManager.INSTANCE.stopFarm(z,"owner typed .autofarm off");
  boolean kept=(Boolean)invoke(m,"tryKeepOnlineOnLogout",z);System.out.println("MANDATORY INFO zenthen kept="+kept+" offlineEnabled=false autofarm=off");
  check(kept&&farming.containsKey(z.getObjectId()),"logout after .autofarm off with stored offlineEnabled=false keeps offline life");
  Player h=online(m,268491948);h.reduceCurrentHp(h.getMaxHp()*10,null,null);check(h.isDead(),"HawkEye is dead before logging out");
  boolean keptDead=(Boolean)invoke(m,"tryKeepOnlineOnLogout",h);System.out.println("MANDATORY INFO dead hawkeye kept="+keptDead);
  check(keptDead,"a dead character also stays in offline life and waits for the revive");
  Player a=online(m,268480957);a.setStoreType(net.sf.l2j.gameserver.enums.actors.StoreType.SELL);
  boolean keptStore=(Boolean)invoke(m,"tryKeepOnlineOnLogout",a);System.out.println("MANDATORY INFO store afrodita kept="+keptStore);
  check(!keptStore,"a private store still keeps the character out of offline life");
  invoke(m,"onServerShutdown");Thread.sleep(500);
  System.out.println("MANDATORY OFFLINE SAVE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
