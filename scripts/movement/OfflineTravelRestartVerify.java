import java.util.*;
import java.nio.file.*;
import java.io.*;
import java.sql.*;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Two actual JVM lifetimes: start a real road, stop, restore its purpose and finish ordinary hunting. */
public class OfflineTravelRestartVerify extends TownUnstuckVerify{
 static final Path EXPECTED=Paths.get("/verify/offline-travel-expected.properties");
 static Object travel(Object m,Player p)throws Exception{return ((Map<?,?>)member(m,"traveling").get(m)).get(p.getObjectId());}
 static void pause(Object clock)throws Exception{invoke(clock,"cancel");Object job=member(clock,"job").get(clock);if(job!=null)suspendValue(job,"join");}
 static void rules()throws Exception{
  Class<?> type=Class.forName("dev.l2j.autobots.movement.HuntJourney");Object a=type.getConstructor(long.class,long.class).newInstance(900L,60L);
  check(((Long)invoke(a,"deadline",100L))==1000L,"initial hunt deadline is absolute");kotlin.Pair<?,?> state=(kotlin.Pair<?,?>)invoke(a,"snapshot");Object b=type.getConstructor(long.class,long.class).newInstance(900L,60L);invoke(b,"restore",state.getFirst(),state.getSecond());
  check(((Long)invoke(b,"deadline",200L))==1000L,"restart cannot grant a fresh hunt budget");check(((Long)invoke(b,"deadline",1000L))==0L,"restored attempt expires at the original deadline");
  state=(kotlin.Pair<?,?>)invoke(b,"snapshot");Object c=type.getConstructor(long.class,long.class).newInstance(900L,60L);invoke(c,"restore",state.getFirst(),state.getSecond());
  check(((Long)invoke(c,"deadline",1059L))==0L,"restart preserves the remaining retry cooldown");check(((Long)invoke(c,"deadline",1060L))==1960L,"expired cooldown starts one normal new attempt");
 }
 static void metadata(Object store)throws Exception{
  Class<?> type=Class.forName("dev.l2j.autobots.offlinelife.OfflineTravelState");Object data=type.getDeclaredConstructors()[0].newInstance("TO_TRAINER",-20000,120000,-3000,30001,true,-22000,140000,-3800,60,true,42L,true,1000L,10000L,900L,0L,new HashSet<Long>(Arrays.asList(3L,7L)));
  invoke(store,"save",2147000000,data);check(data.equals(invoke(store,"load",2147000000)),"SQL round trip preserves every trainer/return/paid-hop/deadline/exclusion field");invoke(store,"clear",2147000000);check(invoke(store,"load",2147000000)==null,"cleared intent does not reappear");
  try(Connection connection=L2DatabaseFactory.getInstance().getConnection();PreparedStatement q=connection.prepareStatement("INSERT INTO character_offline_travel(obj_Id,intent,updated) VALUES(2147000001,?,0)")){q.setString(1,"{\"version\":99}");q.executeUpdate();}
  check(invoke(store,"load",2147000001)==null,"unknown intent version is rejected without blocking startup");
 }
 static long value(Properties p,String key){return Long.parseLong(p.getProperty(key));}
 static void same(Properties expected,Object t,String field)throws Exception{check(String.valueOf(invoke(t,"get"+field)).equals(expected.getProperty(field)),"restart retains "+field);}
 static void save(Object m,Player p,Object t,Object store)throws Exception{
  invoke(m,"onServerShutdown");p.store();Object saved=invoke(store,"load",p.getObjectId());check(saved!=null,"active real road has a durable SQL intent");
  check(invoke(saved,"getTrip").equals(invoke(t,"getTrip").toString()),"stored purpose matches the active road");
  check(invoke(saved,"getStartedAt").equals(invoke(t,"getStartedAt"))&&invoke(saved,"getDeadlineAt").equals(invoke(t,"getDeadlineAt")),"SQL keeps the original movement start and deadline");
  Properties out=new Properties();for(String field:new String[]{"Trip","TargetX","TargetY","TargetZ","StartedAt","DeadlineAt","GkHopped","SpotKey","ArrivalRadius"})out.setProperty(field,String.valueOf(invoke(t,"get"+field)));
  out.setProperty("x",String.valueOf(p.getX()));out.setProperty("y",String.valueOf(p.getY()));out.setProperty("z",String.valueOf(p.getZ()));out.setProperty("exp",String.valueOf(p.getExp()));out.setProperty("cash",String.valueOf(p.getAdena()));out.setProperty("scrolls",String.valueOf(p.getInventory().getInventoryItemCount(736,-1)));
  out.setProperty("journeyStart",String.valueOf(invoke(saved,"getJourneyStart")));out.setProperty("journeyRetry",String.valueOf(invoke(saved,"getJourneyRetry")));out.setProperty("excluded",String.valueOf(invoke(saved,"getExcluded")));
  try(OutputStream file=Files.newOutputStream(EXPECTED)){out.store(file,"Actual first-server checkpoint, never a synthetic arrival");}
  System.out.println("INTENT INFO saved at="+location(p)+" target="+out.getProperty("TargetX")+","+out.getProperty("TargetY")+","+out.getProperty("TargetZ")+" deadline="+out.getProperty("DeadlineAt"));
 }
 public static void main(String[] args){try{
  List<String> flags=Arrays.asList(args);if(flags.contains("--rules-only")){rules();System.out.println("OFFLINE TRAVEL RULES ALL PASSED");return;}
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();boolean resume=flags.contains("--resume"),seed=flags.contains("--seed-check");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");pause(member(m,"travelTask").get(m));pause(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE));
  if(seed){Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"marked road actor restores after startup");check(travel(m,p)!=null,"startup resumes the saved road instead of farming the intermediate position");System.out.println("OFFLINE TRAVEL SEED ALL PASSED");System.exit(0);}
  Object store=singleton("offlinelife.OfflineTravelStore");
  if(!resume){
   rules();metadata(store);check(World.getInstance().getPlayer("Zenthen")==null,"first phase suppresses unrelated automatic startup actions");Player p=Player.restore(268486152);check(p!=null,"original stuck snapshot restores a real actor");
   p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
   ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(store,"clear",p.getObjectId());invoke(m,"markFarming",p.getObjectId());invoke(m,"openJournal",p);invoke(m,"goHunting",p);
   long until=System.currentTimeMillis()+600000,reported=0;Object tracked=null;Location origin=null;
   while(System.currentTimeMillis()<until){
    invoke(m,"tick");if(p.isDead())throw new AssertionError("death before restart checkpoint");Object t=travel(m,p);
    if(t!=null&&invoke(t,"getTrip").toString().equals("TO_SPOT")&&!hasTownAction(m,p)){
     if(tracked!=t){tracked=t;origin=location(p);}
     if(gap(origin,location(p))>=300&&((Long)invoke(t,"getDeadlineAt"))>System.currentTimeMillis()+30000){save(m,p,t,store);System.out.println("OFFLINE TRAVEL SAVED ALL PASSED");System.exit(0);}
    }
    if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();System.out.println("INTENT INFO first server at="+location(p)+" cash="+p.getAdena()+" trip="+(t==null?"none":invoke(t,"getTrip")));}Thread.sleep(500);
   }
   throw new AssertionError("first server failed to start a real hunting road");
  }
  Properties expected=new Properties();try(InputStream file=Files.newInputStream(EXPECTED)){expected.load(file);}Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"normal second-server startup restores the marked offline character");Object t=travel(m,p);check(t!=null,"second server resumes the unfinished road before local farming");
  for(String field:new String[]{"Trip","TargetX","TargetY","TargetZ","StartedAt","DeadlineAt","GkHopped","SpotKey","ArrivalRadius"})same(expected,t,field);
  check(!AutofarmManager.INSTANCE.isAutoFarming(p),"intermediate road position is not mistaken for a hunting anchor");
  check(gap(location(p),new Location((int)value(expected,"x"),(int)value(expected,"y"),(int)value(expected,"z")))<1000,"resume starts near the actual stored position without a free teleport");
  check(p.getAdena()==value(expected,"cash")&&p.getInventory().getInventoryItemCount(736,-1)==value(expected,"scrolls"),"restart cannot charge the already used hop or escape again");
  Object persisted=invoke(store,"load",p.getObjectId());check(persisted!=null,"restored movement remains durable until arrival");check(String.valueOf(invoke(persisted,"getJourneyStart")).equals(expected.getProperty("journeyStart"))&&String.valueOf(invoke(persisted,"getJourneyRetry")).equals(expected.getProperty("journeyRetry")),"whole hunt-chain budget and cooldown survive restart");check(String.valueOf(invoke(persisted,"getExcluded")).equals(expected.getProperty("excluded")),"restart retains failed hunting candidates");
  Location originalGoal=new Location((int)value(expected,"TargetX"),(int)value(expected,"TargetY"),(int)value(expected,"TargetZ"));boolean reachedOriginal=false;
  long initial=p.getExp(),until=System.currentTimeMillis()+600000,reported=0;
  while(System.currentTimeMillis()<until){invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);if(p.isDead())throw new AssertionError("death before resumed road gains hunting XP");
   if(gap(location(p),originalGoal)<=value(expected,"ArrivalRadius")+64)reachedOriginal=true;
   if(reachedOriginal&&p.getExp()>initial&&travel(m,p)==null&&AutofarmManager.INSTANCE.isAutoFarming(p)&&!hasTownAction(m,p))break;
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();System.out.println("INTENT INFO second server at="+location(p)+" exp="+p.getExp()+" hp="+(int)p.getCurrentHp()+" trip="+(travel(m,p)==null?"none":invoke(travel(m,p),"getTrip")));}Thread.sleep(500);}
  check(reachedOriginal,"resumed road physically reaches its original saved goal");check(p.getExp()>initial,"resumed road actually reaches productive ordinary hunting");check(invoke(store,"load",p.getObjectId())==null,"completed road removes the durable intent");check(!p.isGM(),"both server lifetimes use an ordinary player");
  // Create one real nearby-prey order solely to exercise owner handoff while an intent still exists.
  AutofarmManager.INSTANCE.stopFarm(p,"owner handoff fixture");List<?> candidates=(List<?>)invoke(singleton("population.HuntingSpots"),"nearArrival",p,location(p),Collections.emptySet(),8000,false);check(!candidates.isEmpty(),"owner handoff has an actual nearby prey candidate");invoke(m,"startSpotTrip",p,candidates.get(0),Collections.emptyList(),true);check(invoke(store,"load",p.getObjectId())!=null,"new active order is durable before owner handoff");check((Boolean)invoke(m,"onPlayerReentry",p),"normal owner reentry hook takes control");check(invoke(store,"load",p.getObjectId())==null&&World.getInstance().getPlayer(p.getObjectId())==null,"owner handoff removes the unfinished order and old actor");
  System.out.println("INTENT INFO hunting exp="+initial+" -> "+p.getExp());System.out.println("OFFLINE TRAVEL RESUME ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
