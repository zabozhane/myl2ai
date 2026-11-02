import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.scripting.Quest;
import net.sf.l2j.gameserver.scripting.QuestState;
import dev.l2j.autobots.autofarm.AutofarmManager;

public class OfflineTownVerify extends GatekeeperVerify {
    static void erase(Player p,int id) {
        ItemInstance item=p.getInventory().getItemByItemId(id);
        if(item!=null)p.getInventory().destroyItem("Verify",item.getObjectId(),item.getCount(),p,null);
    }
    static void stamp(Object manager,String field,Player p,long time) throws Exception {
        @SuppressWarnings("unchecked") Map<Integer,Long> map=(Map<Integer,Long>)member(manager,field).get(manager);
        map.put(p.getObjectId(),time);
    }
    static boolean hasTownAction(Object manager,Player p)throws Exception {
        return ((Map<?,?>)member(manager,"townJobs").get(manager)).containsKey(p.getObjectId());
    }
    static void cancelTown(Object manager,Player p)throws Exception {
        Object job=((Map<?,?>)member(manager,"townJobs").get(manager)).get(p.getObjectId());
        if(job!=null)invoke(job,"cancel");
        Thread.sleep(300);p.abortCast();p.stopMove(null);
        ((Map<?,?>)member(manager,"traveling").get(manager)).remove(p.getObjectId());
    }
    public static void main(String[] args) {
        try {
            if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
            GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");
            invoke(member(m,"travelTask").get(m),"cancel");
            for(Object j:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(j,"cancel");
            Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
            invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
            Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"offline farmer restored");
            AutofarmManager.INSTANCE.stopFarm(p,"town decision test");
            p.setAccessLevel(8);p.setIsInvul(true);p.getAppearance().setInvisible();
            warp(p,new Location(-40824,-109208,-1808));money(p,2196);
            @SuppressWarnings("unchecked") Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);
            farmers.entrySet().removeIf(e->e.getKey()!=p.getObjectId());
            for(String name:new String[]{"traveling","retryAfter","huntProgress"})((Map<?,?>)member(m,name).get(m)).clear();
            stamp(m,"lessonCheckedAt",p,System.currentTimeMillis()+3600000);
            QuestState state=p.getQuestState("Q273_InvadersOfTheHolyLand");
            if(state==null)state=new QuestState(p,ScriptData.getInstance().getQuest("Q273_InvadersOfTheHolyLand"),Quest.STATE_STARTED);
            state.setState(Quest.STATE_STARTED);
            erase(p,1475);erase(p,1476);p.getInventory().addItem("Verify",1475,10,p,null);
            erase(p,1060);p.getInventory().addItem("Verify",1060,10,p,null);
            check((Integer)invoke(singleton("population.economy.Errands"),"loadPercent",p)<45,"fixture is not overloaded");
            AutofarmManager.INSTANCE.startFarm(p);
            stamp(m,"lastTownTripAt",p,System.currentTimeMillis()-3600000);
            stamp(m,"productiveSince",p,System.currentTimeMillis()-180000);
            stamp(m,"townCheckedAt",p,0);invoke(m,"tick");
            check(!hasTownAction(m,p)&&AutofarmManager.INSTANCE.isAutoFarming(p),"real offline tick keeps hunting with quest loot after only three productive minutes");
            stamp(m,"productiveSince",p,System.currentTimeMillis()-660000);
            stamp(m,"townCheckedAt",p,0);invoke(m,"tick");
            check(hasTownAction(m,p),"real offline tick schedules quest turn-in after useful hunting");
            cancelTown(m,p);
            AutofarmManager.INSTANCE.startFarm(p);
            for(int id:new int[]{1060,1061,1539,1540})erase(p,id);
            stamp(m,"lastTownTripAt",p,System.currentTimeMillis());
            stamp(m,"productiveSince",p,System.currentTimeMillis());
            stamp(m,"townCheckedAt",p,0);invoke(m,"tick");
            check(hasTownAction(m,p)||!AutofarmManager.INSTANCE.isAutoFarming(p),"urgent healing can interrupt a new hunt without waiting for quest timer");
            System.out.println("OFFLINE TOWN ALL PASSED");System.exit(0);
        }catch(Throwable error){error.printStackTrace();System.exit(1);}
    }
}
