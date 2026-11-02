import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.autofarm.AutofarmManager;

public class TravelBudgetVerify extends GatekeeperVerify {
    public static void main(String[] args) {
        try {
            if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
            GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");
            invoke(member(m,"travelTask").get(m),"cancel");
            for(Object j:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(j,"cancel");
            Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
            invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
            Player p=World.getInstance().getPlayer("Zenthen");AutofarmManager.INSTANCE.stopFarm(p,"travel budget test");
            p.setAccessLevel(8);p.setIsInvul(true);p.getAppearance().setInvisible();p.getStat().setLevel((byte)12);
            Object errands=singleton("population.economy.Errands"),travel=singleton("population.economy.GatekeeperTravel");
            beside(p,npc("Papuma"));money(p,10000);
            for(int id:new int[]{736,1060,1061,1539,1540})OfflineTownVerify.erase(p,id);
            p.getInventory().addItem("Verify",1060,10,p,null);
            suspendValue(singleton("offlinelife.OfflineChores"),"buySupplies",p);
            check(p.getInventory().getInventoryItemCount(736,-1)==1,"novice buys one real SoE instead of spending on two");
            p.getInventory().addItem("Verify",736,1,p,null);
            beside(p,npcId(30256));money(p,686);
            check((Integer)invoke(errands,"spendable",p)==0,"ordinary shopping reserve stays protected");
            check((Integer)invoke(errands,"travelSpendable",p)==686,"carried healing and escape supplies permit using cash for a real ride");
            List<?> offers=(List<?>)invoke(travel,"offers",p);
            check(offers.stream().anyMatch(o->{try{return (Integer)invoke(o,"getPrice")==610;}catch(Exception e){throw new RuntimeException(e);}}),"actual nearby gatekeeper offers the affordable 610-adena route");
            Object recovery=singleton("offlinelife.HuntRecovery");Object ride=invoke(recovery,"plan",p,Collections.emptySet());
            check(ride!=null,"prepared level-12 novice finds a reachable hunting region within the real budget");
            int before=p.getAdena();
            check(Boolean.TRUE.equals(suspendValue(recovery,"travel",p,ride)),"hunting recovery uses the actual gatekeeper service");
            int paid=(Integer)invoke(ride,"getPaid");
            check(paid>0&&before-p.getAdena()==paid&&p.getAdena()>=0,"affordable recovery charges once without creating currency");
            beside(p,npcId(30256));money(p,686);
            for(int id:new int[]{1060,1061,1539,1540})OfflineTownVerify.erase(p,id);
            check((Integer)invoke(errands,"travelSpendable",p)==0,"unprepared novice still reserves money for essential supplies");
            System.out.println("TRAVEL BUDGET ALL PASSED");System.exit(0);
        }catch(Throwable error){error.printStackTrace();System.exit(1);}
    }
}
