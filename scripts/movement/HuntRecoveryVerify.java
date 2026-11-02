import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Recovers the real stranded snapshot. Long mode checks real movement and XP without GM protection. */
public class HuntRecoveryVerify extends GatekeeperVerify {
    public static void main(String[] args) {
        try {
            if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
            GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");
            invoke(member(m,"travelTask").get(m),"cancel");
            for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");
            ((Map<?,?>)member(m,"townJobs").get(m)).clear();
            invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
            Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"stranded character exists");
            AutofarmManager.INSTANCE.stopFarm(p,"recovery verification");
            p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
            warp(p,new Location(-14056,121976,-2984));money(p,2196);
            p.setCurrentHp(p.getMaxHp());p.setCurrentMp(p.getMaxMp());
            for(String key:new String[]{"retryAfter","unreachableSpots","emptySpots","failedRoundAt","traveling","huntProgress","pendingHunt"}) {
                Object value=member(m,key).get(m);if(value instanceof Map)((Map<?,?>)value).clear();else if(value instanceof Set)((Set<?>)value).clear();
            }
            @SuppressWarnings("unchecked") Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);
            farmers.entrySet().removeIf(e->e.getKey()!=p.getObjectId());
            // Isolate search/navigation from optional town errands and training; those have separate tests.
            for(String key:new String[]{"lessonCheckedAt","townCheckedAt"}) {
                @SuppressWarnings("unchecked") Map<Integer,Long> times=(Map<Integer,Long>)member(m,key).get(m);
                times.put(p.getObjectId(),System.currentTimeMillis()+3600000);
            }
            long before=p.getExp();long scrolls=p.getInventory().getInventoryItemCount(736,-1);invoke(m,"goHunting",p);
            Map<?,?> travels=(Map<?,?>)member(m,"traveling").get(m);
            Object trip=travels.get(p.getObjectId());
            check(trip!=null,"search reaches a safe route beyond the first five failed candidates");
            check(((List<?>)invoke(trip,"getRoute")).size()>1,"recovery installs a checked multi-point route");
            System.out.println("RECOVERY INFO initial route="+((List<?>)invoke(trip,"getRoute")).size()+" goal="+invoke(trip,"getTargetX")+","+invoke(trip,"getTargetY"));
            if(args.length>0&&args[0].equals("--plan-only")){System.out.println("HUNT SEARCH ALL PASSED");System.exit(0);}
            long deadline=System.currentTimeMillis()+1200000,reported=0;
            while(System.currentTimeMillis()<deadline&&p.getExp()<=before) {
                invoke(m,"tick");
                if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
                if(System.currentTimeMillis()-reported>20000) {
                    reported=System.currentTimeMillis();Object route=travels.get(p.getObjectId());
                    System.out.println("RECOVERY INFO exp="+p.getExp()+" hp="+(int)p.getCurrentHp()+" dead="+p.isDead()+" at="+p.getX()+","+p.getY()+","+p.getZ()+" route="+(route==null?"none":invoke(route,"getRouteIndex")+"/"+((List<?>)invoke(route,"getRoute")).size()));
                }
                Thread.sleep(1000);
            }
            check(p.getExp()>before,"normal character recovers from stranded position and earns real experience");
            check(p.getInventory().getInventoryItemCount(736,-1)==scrolls,"useful recovery walk does not reset itself through SoE");
            System.out.println("HUNT RECOVERY ALL PASSED");System.exit(0);
        }catch(Throwable error){error.printStackTrace();System.exit(1);}
    }
}
