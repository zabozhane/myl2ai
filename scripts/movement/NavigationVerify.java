import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.Unit;
import kotlin.coroutines.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Actual navigation on disposable world + real geodata. GM invisibility isolates movement from combat. */
public class NavigationVerify extends MovementVerify {
    static void suspendRun(Object receiver, String name, Object... args) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Object[] result = new Object[1];
        Continuation<Object> continuation = new Continuation<Object>() {
            public CoroutineContext getContext() { return EmptyCoroutineContext.INSTANCE; }
            public void resumeWith(Object value) { result[0] = value; done.countDown(); }
        };
        Object[] all = Arrays.copyOf(args, args.length + 1);
        all[args.length] = continuation;
        Object value = invoke(receiver, name, all);
        if (value == kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
            check(done.await(180, TimeUnit.SECONDS), "walk completes within test timeout");
            kotlin.ResultKt.throwOnFailure(result[0]);
        }
    }
    static Npc npc(String name) {
        for (Object o : World.getInstance().getObjects())
            if (o instanceof Npc && ((Npc) o).getName().equals(name)) return (Npc) o;
        throw new AssertionError("Missing NPC " + name);
    }
    static Location location(Player p) { return new Location(p.getX(), p.getY(), p.getZ()); }
    static void warp(Player p, Location to) throws Exception {
        p.abortAttack(); p.abortCast(); p.stopMove(null); p.setTarget(null);
        if (p.isSitting()) { p.standUp(); Thread.sleep(3000); }
        p.teleportTo(to, 0); p.onTeleported(); Thread.sleep(300);
    }
    static double gap(Location a, Location b) { return Math.sqrt(Math.pow(a.getX()-b.getX(),2)+Math.pow(a.getY()-b.getY(),2)+Math.pow(a.getZ()-b.getZ(),2)); }
    static void routeValid(Player p, Location origin, Object walk, String label) throws Exception {
        check(walk != null, label + " has a planned path");
        Location previous = origin;
        for (Object o : (List<?>) invoke(walk, "getPoints")) {
            Location point = (Location) o;
            check(GeoEngine.getInstance().canMoveToTarget(previous.getX(),previous.getY(),previous.getZ(),point.getX(),point.getY(),point.getZ()), label + " link respects walls and floors");
            check((Boolean) invoke(singleton("population.RouteSafety"), "safeSegment", p, previous, point), label + " link respects water and dangerous mobs");
            previous = point;
        }
    }
    static void progressChecks() throws Exception {
        Class<?> type = Class.forName("dev.l2j.autobots.movement.RouteProgress");
        Object watch = type.getConstructor(long.class,long.class).newInstance(0L,60000L);
        check(invoke(watch,"observe",0,1000.0,0L)==null, "initial waypoint starts observation");
        check("no_progress".equals(invoke(watch,"observe",0,1000.0,20000L)), "moving flag cannot hide stationary travel");
        watch = type.getConstructor(long.class,long.class).newInstance(0L,60000L);
        check(invoke(watch,"observe",0,1000.0,0L)==null, "detour begins");
        check(invoke(watch,"observe",0,500.0,15000L)==null, "progress to waypoint resets idle timer");
        check(invoke(watch,"observe",1,2000.0,30000L)==null, "a necessary detour can increase distance to final goal");
        check("journey_timeout".equals(invoke(watch,"observe",2,100.0,60000L)), "new waypoints do not reset total journey budget");
    }
    public static void main(String[] args) {
        try {
            if (!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED"))) throw new IllegalStateException("Disposable world required");
            progressChecks();
            GameServer.main(new String[0]);
            Object manager = singleton("offlinelife.OfflineFarmManager");
            invoke(member(manager,"travelTask").get(manager),"cancel");
            for (Object job : ((Map<?,?>) member(manager,"townJobs").get(manager)).values()) invoke(job,"cancel");
            invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
            Player p = World.getInstance().getPlayer("Zenthen");
            check(p!=null,"offline player restored only on isolated world");
            AutofarmManager.INSTANCE.stopFarm(p,"navigation verification");
            p.setAccessLevel(8); p.setIsInvul(true); p.getAppearance().setInvisible();
            Object planner=singleton("population.economy.NpcTravel");
            Object orcWalk=null;
            for (String name : new String[]{"Papuma","Herbiel","Vollodos"}) {
                Npc target=npc(name);
                Location origin=MapRegionData.getInstance().getLocationToTeleport(target,MapRegionData.TeleportType.TOWN);
                warp(p,origin);
                Object walk=invoke(planner,"plan",p,target,location(p),12000);
                routeValid(p,location(p),walk,name);
                if (name.equals("Papuma")) orcWalk=walk;
            }
            // Execute a shared population/NPC walking sequence, not only path planning.
            Npc papuma=npc("Papuma");
            warp(p,MapRegionData.getInstance().getLocationToTeleport(papuma,MapRegionData.TeleportType.TOWN));
            List<?> points=(List<?>) invoke(orcWalk,"getPoints");
            Location end=(Location)points.get(points.size()-1);
            AtomicBoolean failed=new AtomicBoolean(false);
            Class<?> walker=Class.forName("dev.l2j.autobots.behaviors.sequences.WalkToSequence");
            Object sequence=walker.getConstructor(Player.class,Location.class,int.class,kotlin.jvm.functions.Function0.class)
                .newInstance(p,end,20,(kotlin.jvm.functions.Function0<Unit>)()->{failed.set(true);return Unit.INSTANCE;});
            suspendRun(sequence,"definition");
            check(!failed.get() && gap(location(p),end)<=20,"shared walker actually reaches the merchant approach");
            // Live follow-up found a narrow turn whose first waypoint is only 16 units away.
            // Skipping it at a 20-unit radius cuts through the wall and fails immediately.
            warp(p,new Location(10824,15608,-4568));
            Location corridor=new Location(9800,15544,-4568);
            Object tight=invoke(planner,"toPoint",p,location(p),corridor,12000);
            routeValid(p,location(p),tight,"Dark Elf narrow turn");
            failed.set(false);
            sequence=walker.getConstructor(Player.class,Location.class,int.class,kotlin.jvm.functions.Function0.class)
                .newInstance(p,corridor,20,(kotlin.jvm.functions.Function0<Unit>)()->{failed.set(true);return Unit.INSTANCE;});
            suspendRun(sequence,"definition");
            check(!failed.get() && gap(location(p),corridor)<=20,"shared walker preserves the short waypoint at a narrow turn");
            // Exact return from the town in the live incident, through OfflineFarmManager's own tick.
            warp(p,new Location(-44200,-112488,GeoEngine.getInstance().getHeight(-44200,-112488,-240)));
            Location goal=new Location(-40824,-109208,GeoEngine.getInstance().getHeight(-40824,-109208,-240));
            Class<?> tripType=Class.forName("dev.l2j.autobots.offlinelife.OfflineFarmManager$Trip");
            Object back=Enum.valueOf((Class)tripType,"BACK_TO_SPOT");
            Class<?> travelType=Class.forName("dev.l2j.autobots.offlinelife.OfflineFarmManager$Travel");
            Constructor<?> ctor=Arrays.stream(travelType.getDeclaredConstructors()).filter(c->c.getParameterCount()==21).findFirst().get();
            ctor.setAccessible(true);
            Object travel=ctor.newInstance(back,goal.getX(),goal.getY(),goal.getZ(),null,false,0,0,0,600,false,null,false,Collections.emptyList(),System.currentTimeMillis(),null,0,0L,Double.MAX_VALUE,0,0);
            @SuppressWarnings("unchecked") Map<Integer,Object> travels=(Map<Integer,Object>)member(manager,"traveling").get(manager);
            travels.put(p.getObjectId(),travel);
            long deadline=System.currentTimeMillis()+180000, reportAt=0;
            while (travels.get(p.getObjectId())==travel && System.currentTimeMillis()<deadline) {
                invoke(manager,"tick");
                if(System.currentTimeMillis()-reportAt>15000) {
                    reportAt=System.currentTimeMillis();
                    System.out.println("MOVEMENT INFO: offline return at="+p.getX()+","+p.getY()+","+p.getZ()+" route="+invoke(travel,"getRouteIndex")+"/"+((List<?>)invoke(travel,"getRoute")).size());
                }
                Thread.sleep(300);
            }
            check(gap(location(p),goal)<=600 && !travels.containsKey(p.getObjectId()),"offline return actually leaves town and reaches the hunting spot");
            System.out.println("NAVIGATION ALL PASSED");
            System.exit(0);
        } catch(Throwable error) { error.printStackTrace(); System.exit(1); }
    }
}
