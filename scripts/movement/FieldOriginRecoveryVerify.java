import java.util.*;
import java.util.concurrent.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Recorded invalid field height, real five-minute escape and ordinary continuation with no supplied resources. */
public class FieldOriginRecoveryVerify extends TownUnstuckVerify {
 @SuppressWarnings({"unchecked","rawtypes"}) public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object offline=singleton("offlinelife.OfflineFarmManager");invoke(member(offline,"travelTask").get(offline),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");Object manager=singleton("AutobotsManager");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","ork");check(b!=null&&b.getLevel()==1,"recorded novice restores");suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);b.abortAttack();b.abortCast();b.stopMove(null);b.setTarget(null);b.setAccessLevel(0);b.setIsInvul(false);b.getAppearance().setVisible();
  // Initial position is the recorded live failure condition; there is no later position override.
  warp(b,new Location(-45292,-101882,-3444));Location origin=location(b);check(!b.isInsideZone(ZoneId.TOWN)&&Math.abs(GeoEngine.getInstance().getHeight(b.getX(),b.getY(),b.getZ())-b.getZ())>1000,"real geodata confirms the outside-town ground mismatch");check(b.getAdena()==0&&b.getInventory().getInventoryItemCount(736,-1)==0,"actual novice has no money or escape scroll");check(!b.isGM()&&!b.isInvul(),"ordinary actor has no GM protection");
  Object recovery=singleton("movement.TownUnstuck");
  if(Arrays.asList(args).contains("--plan-only")){check(Boolean.TRUE.equals(invoke(recovery,"needsRecovery",b)),"invalid field origin is recognized before a new endless route loop");System.out.println("FIELD ORIGIN PLAN ALL PASSED");System.exit(0);}
  Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);Object session=member(life,"session").get(life);Object hunt=Enum.valueOf((Class)Class.forName("dev.l2j.autobots.population.SessionPlan$Activity"),"Hunt");invoke(session,"switchTo",hunt,30,"recorded field origin recovery");member(life,"tripDoneInTown").set(life,true);
  long xp=b.getExp(),began=System.currentTimeMillis(),castAt=0,arrivedAt=0,reported=began,deadline=began+900000;boolean cleared=false,prepared=false;CompletableFuture<Object> turn=null;
  while(System.currentTimeMillis()<deadline){
   if(turn!=null&&turn.isDone()){turn.get();turn=null;}
   if(b.isDead())throw new AssertionError("novice died during field origin recovery");
   if(b.isCastingNow()&&castAt==0)castAt=System.currentTimeMillis();
   if(arrivedAt==0&&b.isInsideZone(ZoneId.TOWN)&&gap(origin,location(b))>1000&&!b.isTeleporting()){
    arrivedAt=System.currentTimeMillis();check(castAt>0&&arrivedAt-castAt>=290000,"normal five-minute escape really casts before town arrival");check(b.getAdena()==0&&b.getInventory().getInventoryItemCount(736,-1)==0,"escape invents no money or scroll");
   }
   if(arrivedAt>0){if(!(Boolean)member(life,"tripDoneInTown").get(life))cleared=true;if(cleared&&(Boolean)member(life,"tripDoneInTown").get(life))prepared=true;}
   if(arrivedAt>0&&prepared&&b.getExp()>xp&&!b.isInsideZone(ZoneId.TOWN))break;
   if(System.currentTimeMillis()-reported>=20000){reported=System.currentTimeMillis();System.out.println("FIELD ORIGIN INFO seconds="+(reported-began)/1000+" at="+location(b)+" casting="+b.isCastingNow()+" town="+b.isInsideZone(ZoneId.TOWN)+" xp="+b.getExp()+" hp="+(int)b.getCurrentHp()+" cleared="+cleared+" prepared="+prepared);}
   if(turn==null)turn=async(b,"onUpdate");
   Thread.sleep(100);
  }
  check(arrivedAt>0&&cleared&&prepared,"recovery arrives at town and resumes normal preparation");check(b.getExp()>xp&&!b.isInsideZone(ZoneId.TOWN),"ordinary novice actually leaves town and gains experience after recovery");check(!b.isGM()&&!b.isInvul(),"continuation retains ordinary combat rules");System.out.println("FIELD ORIGIN INFO xp="+xp+"->"+b.getExp()+" seconds="+(System.currentTimeMillis()-began)/1000);System.out.println("FIELD ORIGIN RECOVERY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
