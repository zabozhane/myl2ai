// Runs only on an isolated database snapshot and real geodata; never on the live server.
import java.lang.reflect.*;
import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import dev.l2j.autobots.Autobot;

public class MovementVerify {
    static Object singleton(String name) throws Exception {
        return Class.forName("dev.l2j.autobots." + name).getField("INSTANCE").get(null);
    }
    static Object invoke(Object receiver, String name, Object... args) throws Exception {
        for (Class<?> type = receiver.getClass(); type != null; type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if ((method.getName().equals(name) || method.getName().startsWith(name + "$")) && method.getParameterCount() == args.length) {
                    method.setAccessible(true);
                    try { return method.invoke(receiver, args); }
                    catch (InvocationTargetException e) { throw new RuntimeException(name, e.getCause()); }
                }
            }
        }
        throw new NoSuchMethodException(name);
    }
    static Field member(Object receiver, String name) throws Exception {
        Field field = receiver.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        System.out.println("MOVEMENT PASS: " + label);
    }
    public static void main(String[] args) {
        try {
            if (!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))
                throw new IllegalStateException("Requires a disposable database and L2_MOVEMENT_ISOLATED=yes");
            GameServer.main(new String[0]);
            Object manager = singleton("offlinelife.OfflineFarmManager");
            invoke(member(manager, "travelTask").get(manager), "cancel");
            Object dao = singleton("dao.AutobotsDao");
            Collection<?> spots = (Collection<?>) invoke(singleton("population.HuntingSpots"), "getSpots");
            int exercised = 0;
            for (String name : new String[]{"Weapons", "BALiCTiK", "Verzilas", "INKOGNITO"}) {
                Autobot bot = (Autobot) invoke(dao, "loadByName", name);
                check(bot != null, "snapshot contains " + name);
                Class<?> type = Class.forName("dev.l2j.autobots.population.LifeBehavior");
                Object life = type.getConstructor(Autobot.class).newInstance(bot);
                Object candidate = invoke(life, "currentSpot");
                if (candidate == null) {
                    check(member(life, "nextSpotRetryAt").getLong(life) > System.currentTimeMillis(),
                          name + " backs off when no safe candidates exist at the recorded position");
                    check(invoke(life, "currentSpot") == null, name + " does not repeatedly search an unchanged empty area");
                    continue;
                }
                exercised++;
                member(life, "spot").set(life, null);
                @SuppressWarnings("unchecked") Map<Long, Long> excluded = (Map<Long, Long>) member(life, "badSpots").get(life);
                long now = System.currentTimeMillis();
                for (Object spot : spots) excluded.put((Long) invoke(spot, "getKey"), now);
                check(invoke(life, "currentSpot") == null, name + " never bypasses failed-spot exclusions");
                long retryAt = member(life, "nextSpotRetryAt").getLong(life);
                check(retryAt > now, name + " schedules a bounded retry instead of searching every tick");
                excluded.clear();
                check(invoke(life, "currentSpot") == null, name + " respects retry delay even with candidates");
                member(life, "nextSpotRetryAt").setLong(life, 0L);
                check(invoke(life, "currentSpot") != null, name + " can resume selection after retry expires");
            }
            check(exercised >= 1, "a real affected character exercised exclusions and resumed selection with real spawns");
            System.out.println("MOVEMENT RETRY ALL PASSED");
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }
}
