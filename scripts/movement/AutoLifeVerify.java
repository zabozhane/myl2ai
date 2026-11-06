import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.autofarm.AutofarmManager;

/**
 * `.autolife` (owner's request 2026-11-06): a connected character lives the offline life in the client. A move by
 * hand gives control back; a logout with auto life on keeps the same plans going offline.
 */
public class AutoLifeVerify extends OfflineTownVerify {
 static boolean busy(Object m,Player p)throws Exception{return ((Map<?,?>)member(m,"traveling").get(m)).containsKey(p.getObjectId())||((Map<?,?>)member(m,"townJobs").get(m)).containsKey(p.getObjectId())||AutofarmManager.INSTANCE.isAutoFarming(p)||(Boolean)invoke(singleton("offlinelife.OfflineLife"),"owns",p);}
 static boolean marked(Player p)throws Exception{try(java.sql.Connection c=net.sf.l2j.L2DatabaseFactory.getInstance().getConnection();java.sql.PreparedStatement ps=c.prepareStatement("SELECT 1 FROM character_offline_farm WHERE obj_Id=?")){ps.setInt(1,p.getObjectId());try(java.sql.ResultSet rs=ps.executeQuery()){return rs.next();}}}
 static void run(Object m,Player p,long ms)throws Exception{long end=System.currentTimeMillis()+ms;while(System.currentTimeMillis()<end){invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);Thread.sleep(300);}}
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Object cmd=singleton("offlinelife.AutoLifeCommandHandler"),bots=singleton("AutobotsManager");
  Player p=Player.restore(268486152);p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  Map<Integer,Player> farming=(Map<Integer,Player>)member(m,"farming").get(m);Map<Integer,Object> travels=(Map<Integer,Object>)member(m,"traveling").get(m);
  check(!farming.containsKey(p.getObjectId())&&!(Boolean)invoke(m,"isOnlineLife",p),"a connected owner starts without auto life");
  System.out.println("AUTO LIFE INFO at="+location(p)+" level="+p.getLevel()+" class="+p.getClassId());
  check((Boolean)invoke(cmd,"onChat",p,".autolife"),".autolife is a command, not chat");
  check((Boolean)invoke(m,"isOnlineLife",p)&&farming.get(p.getObjectId())==p,".autolife hands the connected character to the life");
  check(marked(p),"auto life is marked in the database: a crash or restart restores the character into its life");
  run(m,p,60000);
  System.out.println("AUTO LIFE INFO after 60 s at="+location(p)+" busy="+busy(m,p)+" trip="+travels.get(p.getObjectId())+" autofarm="+AutofarmManager.INSTANCE.isAutoFarming(p)+" life="+invoke(singleton("offlinelife.OfflineLife"),"purpose",p));
  check(busy(m,p),"the life plays the connected character (travel, errand, hunt or profession)");
  invoke(bots,"onOwnerAction",p,"move");
  check(!(Boolean)invoke(m,"isOnlineLife",p)&&!farming.containsKey(p.getObjectId())&&!travels.containsKey(p.getObjectId())&&!AutofarmManager.INSTANCE.isAutoFarming(p)&&!(Boolean)invoke(singleton("offlinelife.OfflineLife"),"owns",p),"a move by hand gives control back: no life, travel or hunt");
  check(World.getInstance().getPlayer(p.getObjectId())==p&&p.isOnline(),"the character stays in the world");
  check(!marked(p),"taking control back clears the restore mark");
  invoke(bots,"onOwnerAction",p,"move");
  check((Boolean)invoke(cmd,"onChat",p,".autolife on")&&(Boolean)invoke(m,"isOnlineLife",p),".autolife on starts it again");
  run(m,p,30000);Object before=travels.get(p.getObjectId());boolean hunting=AutofarmManager.INSTANCE.isAutoFarming(p);
  check((Boolean)invoke(m,"tryKeepOnlineOnLogout",p),"a logout during auto life keeps the character in the world");
  check(farming.get(p.getObjectId())==p&&!(Boolean)invoke(m,"isOnlineLife",p)&&travels.get(p.getObjectId())==before&&AutofarmManager.INSTANCE.isAutoFarming(p)==hunting,"the same life goes on offline: same trip, same hunt");
  invoke(bots,"onOwnerAction",p,"move");
  check(farming.get(p.getObjectId())==p,"an offline farmer ignores owner-action hooks");
  System.out.println("AUTO LIFE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
