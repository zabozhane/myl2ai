import java.lang.reflect.*;
import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.autofarm.AutofarmManager;

public class ReturnBudgetVerify extends GatekeeperVerify {
    public static void main(String[] args) {
        try {
            if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
            GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");
            invoke(member(m,"travelTask").get(m),"cancel");
            for(Object j:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(j,"cancel");
            Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
            invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
            Player p=World.getInstance().getPlayer("Zenthen");
            AutofarmManager.INSTANCE.stopFarm(p,"return budget test");
            p.setAccessLevel(8);p.setIsInvul(true);p.getAppearance().setInvisible();
            warp(p,new Location(-14056,121976,-2984));money(p,2196);
            Object hs=singleton("population.HuntingSpots"),nav=singleton("population.economy.NpcTravel");
            List<?> candidates=(List<?>)invoke(hs,"nearArrival",p,location(p),Collections.emptySet(),100000,true);
            Object walk=null;
            for(Object candidate:candidates) {
                Location anchor=(Location)invoke(hs,"huntAnchor",p,candidate,location(p),false);
                if(anchor!=null)walk=invoke(nav,"toPoint",p,location(p),anchor,100000);
                if(walk!=null)break;
            }
            check(walk!=null,"a real checked long return route exists");
            List<?> points=(List<?>)invoke(walk,"getPoints");Location goal=(Location)points.get(points.size()-1);
            Class<?> tripType=Class.forName("dev.l2j.autobots.offlinelife.OfflineFarmManager$Trip");
            Object back=Enum.valueOf((Class)tripType,"BACK_TO_SPOT");
            Class<?> type=Class.forName("dev.l2j.autobots.offlinelife.OfflineFarmManager$Travel");
            Constructor<?> ctor=Arrays.stream(type.getDeclaredConstructors()).filter(c->c.getParameterCount()==21).findFirst().get();ctor.setAccessible(true);
            Object travel=ctor.newInstance(back,goal.getX(),goal.getY(),goal.getZ(),null,false,0,0,0,600,false,null,false,points,System.currentTimeMillis()-301000,null,0,0L,Double.MAX_VALUE,0,0);
            @SuppressWarnings("unchecked") Map<Integer,Object> travels=(Map<Integer,Object>)member(m,"traveling").get(m);
            travels.put(p.getObjectId(),travel);invoke(m,"tick");
            check(travels.get(p.getObjectId())==travel,"useful return remains active beyond the ordinary five-minute limit");
            Object progress=invoke(travel,"getProgress");long budget=member(progress,"budgetMs").getLong(progress);
            check(budget>300000&&budget<=900000,"long return has a calculated finite recovery budget");
            System.out.println("RETURN BUDGET ALL PASSED");System.exit(0);
        }catch(Throwable error){error.printStackTrace();System.exit(1);}
    }
}
