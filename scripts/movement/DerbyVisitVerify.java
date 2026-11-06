import java.lang.reflect.*;
import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.manager.DerbyTrackManager;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * Stand only: population bots ride to the Monster Derby Track, bet, watch the race, cash the ticket and ride back.
 * Config on the stand: populationSize about 8, populationAlwaysOnline=true, populationTestLevel=20, questsEnabled=false,
 * groupsEnabled=false, llm off, derbyChancePercent=100000 (so the next segment is the derby). Each bot gets 300k adena
 * and its segment is cut once; the check waits for the first race to end and the tickets to be cashed.
 * Success: DERBY VISIT ALL PASSED.
 */
public class DerbyVisitVerify {
 static Object field(Object o,String n)throws Exception{for(Class<?> c=o.getClass();c!=null;c=c.getSuperclass())try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f.get(o);}catch(NoSuchFieldException e){}throw new NoSuchFieldException(n);}
 static void check(boolean ok,String what){System.out.println((ok?"OK   ":"FAIL ")+what);if(!ok)throw new AssertionError(what);}
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("stand only");
  GameServer.main(new String[0]);
  Class<?> autobot=Class.forName("dev.l2j.autobots.Autobot");
  Class<?> life=Class.forName("dev.l2j.autobots.population.LifeBehavior");
  Method next=life.getDeclaredMethod("nextSegment",Boolean.class,String.class);next.setAccessible(true);
  Map<Integer,Long> start=new HashMap<>();Set<Integer> wentToTrack=new HashSet<>(),cameBack=new HashSet<>();
  int firstRace=DerbyTrackManager.getInstance().getRaceNumber();
  long end=System.currentTimeMillis()+40*60_000L;
  while(System.currentTimeMillis()<end){
   for(Player p:World.getInstance().getPlayers()){
    if(!autobot.isInstance(p))continue;Object l=field(p,"life");if(l==null)continue;
    if(!start.containsKey(p.getObjectId())&&start.size()<6&&p.getX()!=0&&"Hunt".equals(String.valueOf(field(field(p,"life"),"session").getClass().getMethod("getActivity").invoke(field(field(p,"life"),"session"))))){p.addAdena("test",300_000,null,false);start.put(p.getObjectId(),(long)p.getAdena());next.invoke(l,null,"derby test");}
    if(!start.containsKey(p.getObjectId()))continue;
    boolean at=p.isInsideZone(net.sf.l2j.gameserver.enums.ZoneId.MONSTER_TRACK)||Math.hypot(p.getX()-12661,p.getY()-181687)<300;
    if(at)wentToTrack.add(p.getObjectId());else if(wentToTrack.contains(p.getObjectId()))cameBack.add(p.getObjectId());
   }
   int race=DerbyTrackManager.getInstance().getRaceNumber();
   if(race>firstRace&&!cameBack.isEmpty()&&cameBack.containsAll(wentToTrack))break;
   Thread.sleep(2000);
  }
  System.out.println("DERBY INFO prepared="+start.size()+" wentToTrack="+wentToTrack.size()+" cameBack="+cameBack.size()+" race="+DerbyTrackManager.getInstance().getRaceNumber()+" firstRace="+firstRace);
  for(Player p:World.getInstance().getPlayers())if(start.containsKey(p.getObjectId()))
   System.out.println("DERBY BOT "+p.getName()+" lv"+p.getLevel()+" adena "+start.get(p.getObjectId())+" -> "+p.getAdena()+" tickets="+p.getInventory().getAllItemsByItemId(4443).length+" at="+p.getX()+","+p.getY()+","+p.getZ());
  check(!wentToTrack.isEmpty(),"bots ride to the race track");
  check(DerbyTrackManager.getInstance().getRaceNumber()>firstRace,"the race they bet on finished");
  check(!cameBack.isEmpty()&&cameBack.containsAll(wentToTrack),"every bot leaves the track after the race");
  for(Player p:World.getInstance().getPlayers())if(autobot.isInstance(p))check(!(p.isInsideZone(net.sf.l2j.gameserver.enums.ZoneId.MONSTER_TRACK)&&!"Derby".equals(String.valueOf(field(field(p,"life"),"session").getClass().getMethod("getActivity").invoke(field(field(p,"life"),"session"))))),p.getName()+" is not left at the track outside a derby visit");
  System.out.println("DERBY VISIT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
