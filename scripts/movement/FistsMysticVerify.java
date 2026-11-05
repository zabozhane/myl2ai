import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.sf.l2j.gameserver.GameServer;
import dev.l2j.autobots.Autobot;

/** Live resident Orc Mystic Voviks (lv 7, no attack spell, died 4 times in 10 min): weaker prey, then ordinary updates for 5 minutes. */
public class FistsMysticVerify extends IslandRecoveryVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Voviks");check(b!=null&&b.getSkill(1090)==null,"Voviks restores without Life Drain");
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());b.setAccessLevel(0);b.setIsInvul(false);
  Object range=invoke(singleton("population.HuntingSpots"),"targetLevels",b);System.out.println("FISTS INFO level="+b.getLevel()+" targetLevels="+range+" ordinary="+invoke(life,"getOrdinaryTargetLevel"));
  check(((kotlin.ranges.IntRange)range).getLast()<=b.getLevel()-1,"prey stays below the fists-only mystic's own level");
  long exp=b.getExp();int deaths=0;boolean wasDead=false;AtomicReference<Throwable> error=new AtomicReference<>();long began=System.currentTimeMillis(),reported=0;
  while(System.currentTimeMillis()-began<300000){update(b,error);if(error.get()!=null)throw new RuntimeException(error.get());
   if(b.isDead()&&!wasDead)deaths++;wasDead=b.isDead();if(b.isDead()){b.doRevive();}
   if(System.currentTimeMillis()-reported>30000){reported=System.currentTimeMillis();System.out.println("FISTS INFO seconds="+(reported-began)/1000+" at="+location(b)+" exp="+b.getExp()+" hp="+(int)b.getCurrentHp()+" target="+(b.getTarget()==null?"-":b.getTarget().getName()));}Thread.sleep(300);}
  System.out.println("FISTS RESULT deaths="+deaths+" exp="+exp+"->"+b.getExp());
  System.out.println("FISTS MYSTIC ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
