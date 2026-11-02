import java.util.*;
import java.lang.reflect.*;
import java.util.concurrent.*;
import kotlin.coroutines.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.location.*;
import net.sf.l2j.gameserver.data.xml.TeleportLocationData;
import dev.l2j.autobots.autofarm.AutofarmManager;

public class GatekeeperVerify extends NavigationVerify {
    static Object suspendValue(Object receiver,String name,Object... args) throws Exception {
        CountDownLatch done=new CountDownLatch(1); Object[] result=new Object[1];
        Continuation<Object> cont=new Continuation<Object>() {
            public CoroutineContext getContext(){return EmptyCoroutineContext.INSTANCE;}
            public void resumeWith(Object value){result[0]=value;done.countDown();}
        };
        Object[] all=Arrays.copyOf(args,args.length+1);all[args.length]=cont;
        Object value=invoke(receiver,name,all);
        if(value==kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
            check(done.await(150,TimeUnit.SECONDS),"gatekeeper action completes");
            kotlin.ResultKt.throwOnFailure(result[0]);return result[0];
        }
        return value;
    }
    static void money(Player p,int amount) {
        ItemInstance adena=p.getInventory().getItemByItemId(57);
        if(adena!=null)p.getInventory().destroyItem("Verify",adena.getObjectId(),adena.getCount(),p,null);
        if(amount>0)p.addAdena("Verify",amount,null,false);
    }
    static Npc npcId(int id) {
        for(Object o:World.getInstance().getObjects())if(o instanceof Npc&&((Npc)o).getNpcId()==id)return (Npc)o;
        throw new AssertionError("Missing NPC "+id);
    }
    static void beside(Player p,Npc npc) throws Exception {warp(p,new Location(npc.getX()+40,npc.getY()+40,npc.getZ()));}
    public static void main(String[] args) {
        try {
            if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
            GameServer.main(new String[0]);Object manager=singleton("offlinelife.OfflineFarmManager");
            invoke(member(manager,"travelTask").get(manager),"cancel");
            for(Object job:((Map<?,?>)member(manager,"townJobs").get(manager)).values())invoke(job,"cancel");
            invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
            Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"snapshot restores Zenthen");
            AutofarmManager.INSTANCE.stopFarm(p,"gatekeeper verification");
            p.setAccessLevel(8);p.setIsInvul(true);p.getAppearance().setInvisible();
            Object service=singleton("population.economy.GatekeeperTravel"),trainers=singleton("population.skills.TrainerTravel");
            money(p,10000);warp(p,new Location(-20824,-115992,-2984));Location field=location(p);
            check(((List<?>)invoke(service,"offers",p)).isEmpty(),"a field hunter has no remote gatekeeper service");
            Object plan=invoke(trainers,"select",p,Collections.emptySet());
            check(plan==null||invoke(plan,"getExit")==null,"field training never chooses a free world teleport to Lakan");
            check(gap(field,location(p))==0,"planning cannot teleport or charge the character");
            beside(p,npc("Papuma"));money(p,10000);
            plan=invoke(trainers,"select",p,Collections.emptySet());
            check(plan!=null&&invoke(plan,"getExit")==null,"Orc Village uses a reachable local trainer");
            Npc bella=npcId(30256);beside(p,bella);money(p,100000);
            List<?> offers=(List<?>)invoke(service,"offers",p);Set<?> ids=(Set<?>)invoke(service,"menuIds",bella);
            check(!offers.isEmpty(),"Bella has real affordable menu destinations");
            for(Object offer:offers)check(ids.contains(invoke(offer,"getId")),"every quoted destination belongs to this gatekeeper");
            Class<?> offerType=Class.forName("dev.l2j.autobots.population.economy.GatekeeperTravel$Offer");
            @SuppressWarnings("unchecked") Map<Integer,TeleportLocation> all=(Map<Integer,TeleportLocation>)member(TeleportLocationData.getInstance(),"_teleports").get(TeleportLocationData.getInstance());
            Map.Entry<Integer,TeleportLocation> foreign=all.entrySet().stream().filter(e->!ids.contains(e.getKey())&&!e.getValue().isNoble()&&e.getValue().getPrice()==0).findFirst().get();
            Object fake=offerType.getConstructor(Npc.class,int.class,TeleportLocation.class,int.class).newInstance(bella,foreign.getKey(),foreign.getValue(),0);
            Location before=location(p);int wallet=p.getAdena();
            check(Boolean.FALSE.equals(suspendValue(service,"travel",p,fake)),"a valid world entry absent from this menu is refused");
            check(gap(before,location(p))==0&&p.getAdena()==wallet,"refused destination neither charges nor teleports");
            Object offer=offers.stream().filter(o->{try{return (Integer)invoke(o,"getPrice")>0;}catch(Exception e){throw new RuntimeException(e);}}).findFirst().get();
            int expected=(Integer)invoke(offer,"getPrice");
            check(Boolean.TRUE.equals(suspendValue(service,"travel",p,offer)),"real gatekeeper core dialog performs an offered ride");
            check(wallet-p.getAdena()==expected,"core charges the actual quoted fare once");
            check(gap(location(p),(Location)invoke(offer,"getExit"))<=200,"character reaches the paid destination");
            beside(p,bella);money(p,0);before=location(p);
            check(Boolean.FALSE.equals(suspendValue(service,"travel",p,offer)),"balance is checked again after planning");
            check(p.getAdena()==0&&gap(before,location(p))==0,"unaffordable ride does not move or create currency");
            System.out.println("GATEKEEPER ALL PASSED");System.exit(0);
        }catch(Throwable error){error.printStackTrace();System.exit(1);}
    }
}
