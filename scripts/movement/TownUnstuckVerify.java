import java.util.*;
import java.util.concurrent.*;
import kotlin.coroutines.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Reproduce the actual sealed Gludio cell. No warps/resources for the snapshot farmer. */
public class TownUnstuckVerify extends OfflineTownVerify {
 static CompletableFuture<Object> async(Object target,String name,Object... args)throws Exception{
  CompletableFuture<Object> done=new CompletableFuture<>();
  Continuation<Object> cont=new Continuation<Object>(){
   public CoroutineContext getContext(){return EmptyCoroutineContext.INSTANCE;}
   public void resumeWith(Object value){try{kotlin.ResultKt.throwOnFailure(value);done.complete(value);}catch(Throwable t){done.completeExceptionally(t);}}
  };
  Object[] all=Arrays.copyOf(args,args.length+1);all[args.length]=cont;Object value=invoke(target,name,all);
  if(value!=kotlin.coroutines.intrinsics.IntrinsicsKt.getCOROUTINE_SUSPENDED())done.complete(value);return done;
 }
 static void continueAtNpc(Player p)throws Exception{
  // Town return uses random stock spawn points. Exercise a local reachable service from the actual arrival.
  List<net.sf.l2j.gameserver.model.actor.Npc> local=new ArrayList<>();
  for(Object o:World.getInstance().getObjects())if(o instanceof net.sf.l2j.gameserver.model.actor.instance.Folk){
   net.sf.l2j.gameserver.model.actor.Npc n=(net.sf.l2j.gameserver.model.actor.Npc)o;
   if(Math.hypot(n.getX()-p.getX(),n.getY()-p.getY())<1500)local.add(n);
  }
  local.sort(Comparator.comparingDouble(n->Math.hypot(n.getX()-p.getX(),n.getY()-p.getY())));
  int attempts=0;Object travel=singleton("population.economy.NpcTravel");
  for(net.sf.l2j.gameserver.model.actor.Npc n:local){
   Object route=invoke(travel,"plan",p,n,location(p),2000);if(route==null)continue;
   System.out.println("UNSTUCK INFO local service="+n.getName()+" distance="+invoke(route,"getLength"));
   if(Boolean.TRUE.equals(suspendValue(travel,"reach",p,n,2000))){
    check(n.isInsideRadius(p,net.sf.l2j.gameserver.model.actor.Npc.INTERACTION_DISTANCE,true,false),"recovery physically continues beside a real local NPC");n.onAction(p);return;
   }
   if(++attempts>=3)break;
  }
  throw new AssertionError("No actual local NPC arrival after recovery");
 }
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  boolean noScroll=Arrays.asList(args).contains("--no-scroll"),hunt=Arrays.asList(args).contains("--hunt");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  check(World.getInstance().getPlayer("Zenthen")==null,"fixture suppresses automatic startup actions before controlled restore");
  Player p=Player.restore(268486152);check(p!=null&&p.getName().equals("Zenthen"),"real stuck snapshot restores Zenthen");
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);
  invoke(AutofarmManager.INSTANCE,"quiesce",p);p.abortCast();p.stopMove(null);p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
  ((Map<Integer,Player>)member(m,"farming").get(m)).entrySet().removeIf(e->e.getKey()!=p.getObjectId());
  for(String field:new String[]{"traveling","retryAfter","pendingHunt","huntJourneys"}){Object c=member(m,field).get(m);if(c instanceof Map)((Map<?,?>)c).clear();else ((Set<?>)c).clear();}
  check(p.getX()==-15048&&p.getY()==124424,"fixture starts at the exact live sealed cell");
  Object recovery=singleton("movement.TownUnstuck");check((Boolean)invoke(recovery,"isSealed",p),"fine geodata flood confirms a closed pocket");
  if(noScroll)erase(p,736); // Deliberate no-scroll fixture; no gifts or accelerated cast.
  long scrolls=p.getInventory().getInventoryItemCount(736,-1),initialExp=p.getExp();Location origin=location(p);int cash=p.getAdena();
  long start=System.currentTimeMillis();invoke(m,"goHunting",p);check(hasTownAction(m,p),"normal offline planner owns recovery instead of repeatedly rejecting spots");
  if(!hunt)stamp(m,"retryAfter",p,start+3600000); // Isolate recovery/NPC continuation from a later paid hunting ride.

  long deadline=start+(noScroll?350000:50000),reported=0;
  while(System.currentTimeMillis()<deadline&&(gap(origin,location(p))<=32||p.isTeleporting()||!p.isInsideZone(ZoneId.TOWN))){
   invoke(m,"tick"); // The ordinary offline tick acknowledges core teleports without a client.
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();System.out.println("UNSTUCK INFO elapsed="+(reported-start)/1000+" casting="+p.isCastingNow()+" at="+location(p));}
   Thread.sleep(500);
  }
  long elapsed=System.currentTimeMillis()-start;
  check(gap(origin,location(p))>32&&p.isInsideZone(ZoneId.TOWN),"ordinary core return actually moves out of the sealed cell");
  check(!(Boolean)invoke(recovery,"isSealed",p),"arrival has a geodata exit");
  check(p.getAdena()==cash,"unstuck creates and spends no money");
  check(p.getInventory().getInventoryItemCount(736,-1)==(noScroll?0:scrolls-1),"only an owned scroll can be consumed");
  if(noScroll)check(elapsed>=290000,"ordinary unstuck honors the five-minute cast");
  if(!hunt){
   // Addressed recovery check. --hunt separately keeps the complete battle scenario.
   while(hasTownAction(m,p))Thread.sleep(100);
   ((Map<?,?>)member(m,"traveling").get(m)).remove(p.getObjectId());p.stopMove(null);
   continueAtNpc(p);
   check(!p.isGM(),"offline recovery uses no GM protection");
   if(noScroll){System.out.println("TOWN UNSTUCK NO SCROLL ALL PASSED");System.exit(0);}
  }
  if(hunt){
  deadline=System.currentTimeMillis()+900000;reported=0;
  while(System.currentTimeMillis()<deadline&&p.getExp()<=initialExp){
   invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
   if(p.isDead()){
    for(net.sf.l2j.gameserver.model.actor.Creature c:p.getKnownTypeInRadius(net.sf.l2j.gameserver.model.actor.Creature.class,1500))if(c.getTarget()==p)
     System.out.println("UNSTUCK INFO death attacker="+c.getName()+" lv="+c.getLevel()+" at="+c.getX()+","+c.getY()+","+c.getZ());
    throw new AssertionError("death before recovery reaches useful hunting");
   }
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();System.out.println("UNSTUCK INFO hunting at="+location(p)+" exp="+p.getExp()+" hp="+(int)p.getCurrentHp()+" cash="+p.getAdena());}Thread.sleep(500);
  }
  check(p.getExp()>initialExp&&!p.isGM(),"recovered offline farmer reaches prey and gains real XP without GM protection");
  }
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Weapons");check(b!=null,"population fixture exists");
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());
  b.setAccessLevel(0);b.setIsInvul(false);b.getAppearance().setVisible();warp(b,origin);
  if(b.getInventory().getInventoryItemCount(736,-1)==0)b.getInventory().addItem("Isolated population fixture",736,1,b,null);
  long owned=b.getInventory().getInventoryItemCount(736,-1);Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);
  async(life,"hunt").get(60,TimeUnit.SECONDS);
  check(gap(origin,location(b))>32&&!(Boolean)invoke(recovery,"isSealed",b),"ordinary population hunting also recovers from the same cell");
  check(b.getInventory().getInventoryItemCount(736,-1)==owned-1,"population recovery consumes its fixture scroll");
  continueAtNpc(b);
  check(!(Boolean)invoke(recovery,"needsRecovery",b),"reachable town street does not start another recovery");
  System.out.println("UNSTUCK INFO finalExp="+p.getExp()+" initialExp="+initialExp+" elapsed="+(System.currentTimeMillis()-start)/1000);
  System.out.println("TOWN UNSTUCK ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
