import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Owner, 2026-11-06: the auto hunt left herbs on the ground. A wounded hunter grabs a herb before it sits down to rest. */
public class HerbLootVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  AutofarmManager.INSTANCE.startFarm(p);p.setCurrentHp(p.getMaxHp()*0.30);
  ItemInstance herb=ItemInstance.create(8600,1,null,null);herb.dropMe(null,p.getX()+150,p.getY(),p.getZ());
  System.out.println("HERB INFO at="+location(p)+" hp="+(int)p.getCurrentHp()+"/"+p.getMaxHp()+" herb="+herb.getName()+" at "+herb.getX()+","+herb.getY());
  long end=System.currentTimeMillis()+20000;boolean sat=false;
  while(System.currentTimeMillis()<end&&herb.isVisible()){suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);if(p.isSitting())sat=true;Thread.sleep(300);}
  System.out.println("HERB RESULT picked="+!herb.isVisible()+" satFirst="+sat+" hp="+(int)p.getCurrentHp());
  check(!herb.isVisible(),"the wounded hunter picks up the herb");
  check(!sat,"it does not sit down to rest before the herb");
  System.out.println("HERB LOOT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
