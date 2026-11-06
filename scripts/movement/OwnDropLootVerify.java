import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Owner, 2026-11-06: a caster on auto hunt left its own drops lying where the monster died, 600-900 away. */
public class OwnDropLootVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  AutofarmManager.INSTANCE.startFarm(p);
  ItemInstance herb=ItemInstance.create(1864,3,null,null);herb.setDropProtection(p.getObjectId(),false);herb.dropMe(null,p.getX()+800,p.getY(),p.getZ());
  System.out.println("DROP INFO at="+location(p)+" hp="+(int)p.getCurrentHp()+"/"+p.getMaxHp()+" herb="+herb.getName()+" at "+herb.getX()+","+herb.getY());
  long end=System.currentTimeMillis()+20000;boolean sat=false;
  while(System.currentTimeMillis()<end&&herb.isVisible()){suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);if(p.isSitting())sat=true;Thread.sleep(300);}
  System.out.println("DROP RESULT picked="+!herb.isVisible()+" satFirst="+sat+" hp="+(int)p.getCurrentHp());
  check(!herb.isVisible()&&p.getInventory().getItemByItemId(1864)!=null,"the hunter fetches its own drop lying 800 away");
  System.out.println("OWN DROP LOOT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
