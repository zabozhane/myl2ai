import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import kotlin.coroutines.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;

/** Administrative fixture repair, followed by ordinary complete bot updates without GM protection. */
public class IslandRecoveryVerify extends GatekeeperVerify{
 static void update(Autobot b,AtomicReference<Throwable> error)throws Exception{
  Continuation<Object> cont=new Continuation<Object>(){public CoroutineContext getContext(){return EmptyCoroutineContext.INSTANCE;}public void resumeWith(Object result){try{kotlin.ResultKt.throwOnFailure(result);}catch(Throwable t){error.set(t);}}};
  invoke(b,"onUpdate",cont);
 }
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");for(Object j:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(j,"cancel");
  List<Autobot> bots=new ArrayList<>();Map<String,Long> initial=new HashMap<>();
  for(String name:new String[]{"Weapons","BALiCTiK","Verzilas"}){
   Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName",name);check(b!=null,"snapshot restores "+name);
   check(b.getLevel()<20&&b.getClassId().getId()==38&&b.getX()>=9000&&b.getX()<=12500&&b.getY()>=-28000&&b.getY()<=-22000,"fixture matches precisely scoped stranded novice "+name);
   Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);
   suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());
   int cash=b.getAdena(),level=b.getLevel();long exp=b.getExp();List<String> inventory=new ArrayList<>();
   for(net.sf.l2j.gameserver.model.item.instance.ItemInstance item:b.getInventory().getItems())inventory.add(item.getObjectId()+":"+item.getItemId()+":"+item.getCount());Collections.sort(inventory);
   // One-time fixture correction only. Production repair must use the same guarded IDs and a backup.
   Location start=b.getTemplate().getRandomSpawn();warp(b,start);
   b.setAccessLevel(0);b.setIsInvul(false);b.getAppearance().setVisible();
   List<String> after=new ArrayList<>();for(net.sf.l2j.gameserver.model.item.instance.ItemInstance item:b.getInventory().getItems())after.add(item.getObjectId()+":"+item.getItemId()+":"+item.getCount());Collections.sort(after);
   check(cash==b.getAdena()&&level==b.getLevel()&&exp==b.getExp()&&inventory.equals(after),name+" recovery changes only position");
   bots.add(b);initial.put(name,exp);System.out.println("ISLAND INFO restored="+name+" at="+location(b)+" exp="+exp+" cash="+cash);
  }
  AtomicReference<Throwable> error=new AtomicReference<>();long deadline=System.currentTimeMillis()+600000,reported=0;
  while(System.currentTimeMillis()<deadline&&bots.stream().anyMatch(b->b.getExp()<=initial.get(b.getName()))){
   for(Autobot b:bots)if(b.getExp()<=initial.get(b.getName()))update(b,error);
   if(error.get()!=null)throw new RuntimeException("ordinary bot update",error.get());
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();for(Autobot b:bots)System.out.println("ISLAND INFO name="+b.getName()+" at="+location(b)+" exp="+b.getExp()+" hp="+(int)b.getCurrentHp()+" dead="+b.isDead());}
   Thread.sleep(1000);
  }
  for(Autobot b:bots)check(b.getExp()>initial.get(b.getName()),b.getName()+" resumes ordinary play and gains experience without GM protection");
  System.out.println("ISLAND RECOVERY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
