import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;

/** After repair-rjaray-20261103.sql on the fixture DB: ordinary complete bot updates must reach prey and gain XP. */
public class RjaRayRepairVerify extends IslandRecoveryVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","RjaRay");check(b!=null,"snapshot restores RjaRay");
  check(b.getLevel()==5&&b.getClassId().getId()==53&&Math.abs(b.getX()-108644)<5&&Math.abs(b.getY()+173947)<5,"repair SQL placed RjaRay on the dwarf starter spawn");
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());
  b.setAccessLevel(0);b.setIsInvul(false);long exp=b.getExp();int cash=b.getAdena();
  check(exp==3692&&cash==21,"level, experience and money are unchanged by the repair");
  AtomicReference<Throwable> error=new AtomicReference<>();long began=System.currentTimeMillis(),deadline=began+600000,reported=0;
  while(System.currentTimeMillis()<deadline&&b.getExp()<=exp&&!b.isDead()){update(b,error);if(error.get()!=null)throw new RuntimeException("ordinary bot update",error.get());
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();System.out.println("RJA REPAIR INFO seconds="+(reported-began)/1000+" at="+location(b)+" exp="+b.getExp()+" hp="+(int)b.getCurrentHp());}Thread.sleep(300);}
  System.out.println("RJA REPAIR RESULT seconds="+(System.currentTimeMillis()-began)/1000+" at="+location(b)+" exp="+exp+"->"+b.getExp()+" dead="+b.isDead()+" cash="+cash+"->"+b.getAdena());
  check(!b.isDead()&&b.getExp()>exp,"RjaRay hunts and gains experience after the repair");check(!b.isGM()&&!b.isInvul(),"no GM or invulnerability");
  System.out.println("RJARAY REPAIR ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
