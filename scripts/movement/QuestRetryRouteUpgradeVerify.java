import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Prior routing-version refusal is produced by the old JVM, then re-evaluated by the changed route policy. */
public class QuestRetryRouteUpgradeVerify extends ProfessionRetryMemoryVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");Player p=Player.restore(268486152);check(p!=null&&p.getLevel()==19,"prior JVM stored the controlled profession actor");Object store=singleton("population.quests.QuestRetryStore"),old=invoke(store,"load",p.getObjectId());check(old!=null&&"not taken".equals(invoke(old,"getStage")),"old algorithm persisted its real failure metadata");check((Boolean)invoke(runner(p),"professionBusiness"),"changed routing capability permits re-evaluating the old refusal");check(invoke(store,"load",p.getObjectId())==null,"obsolete failure context is removed without changing quest progress");System.out.println("QUEST RETRY ROUTE UPGRADE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
