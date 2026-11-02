import java.util.*;
import java.lang.reflect.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Finish the real city checkpoint from TownWalkVerify without repeating its completed road. */
public class TownSupplyCheckpointVerify extends OfflineTownVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable checkpoint required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=World.getInstance().getPlayer("Zenthen");check(p!=null&&p.isInsideZone(ZoneId.TOWN),"completed road checkpoint is really stored in town");
  invoke(AutofarmManager.INSTANCE,"quiesce",p);p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
  @SuppressWarnings("unchecked") Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);farmers.entrySet().removeIf(e->e.getKey()!=p.getObjectId());
  ((Map<?,?>)member(m,"traveling").get(m)).clear();
  // Restore only the fixture's intended return destination. No position, items, money or XP are changed.
  Location origin=new Location(-37096,-118374,GeoEngine.getInstance().getHeight(-37096,-118374,-1760));
  int before=p.getAdena();long exp=p.getExp();check(before>=1490,"checkpoint retains its actual earned shopping budget");
  invoke(m,"runChoresThenBack",p,origin);long deadline=System.currentTimeMillis()+240000,reported=0;Object back=null;
  while(System.currentTimeMillis()<deadline){invoke(m,"tick");back=((Map<?,?>)member(m,"traveling").get(m)).get(p.getObjectId());
   if(p.isDead())throw new AssertionError("checkpoint shopping died");
   if(back!=null&&invoke(back,"getTrip").toString().equals("BACK_TO_SPOT")&&p.getInventory().getInventoryItemCount(1060,-1)>0&&p.getInventory().getInventoryItemCount(736,-1)==1)break;
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();System.out.println("TOWN CHECKPOINT INFO at="+location(p)+" cash="+p.getAdena()+" potions="+p.getInventory().getInventoryItemCount(1060,-1)+" escape="+p.getInventory().getInventoryItemCount(736,-1));}
   Thread.sleep(300);
  }
  check(back!=null&&invoke(back,"getTrip").toString().equals("BACK_TO_SPOT"),"completed shopping restores its intended return to the spot");
  check(p.getAdena()<before&&p.getInventory().getInventoryItemCount(1060,-1)>0&&p.getInventory().getInventoryItemCount(736,-1)==1,"actual town transactions buy healing and one escape scroll");
  check(!p.isGM()&&p.getExp()==exp,"checkpoint completion uses no GM protection or invented progress");
  ((Map<?,?>)member(m,"traveling").get(m)).clear();p.stopMove(null);
  long productive=System.currentTimeMillis()-660000,last=System.currentTimeMillis()-3600000;stamp(m,"productiveSince",p,productive);stamp(m,"lastTownTripAt",p,last);
  Class<?> tripType=Class.forName("dev.l2j.autobots.offlinelife.OfflineFarmManager$Trip"),type=Class.forName("dev.l2j.autobots.offlinelife.OfflineFarmManager$Travel");
  Constructor<?> ctor=Arrays.stream(type.getDeclaredConstructors()).filter(c->c.getParameterCount()==21).findFirst().get();ctor.setAccessible(true);
  Object failed=ctor.newInstance(Enum.valueOf((Class)tripType,"TO_TOWN"),p.getX(),p.getY(),p.getZ(),null,false,0,0,0,600,false,null,false,Collections.emptyList(),System.currentTimeMillis(),null,0,0L,Double.MAX_VALUE,0,0);
  invoke(m,"onStuck",p,failed);
  check(((Long)((Map<?,?>)member(m,"townRetryAfter").get(m)).get(p.getObjectId()))>System.currentTimeMillis(),"failed town route has a finite retry time");
  check(((Map<?,?>)member(m,"productiveSince").get(m)).get(p.getObjectId()).equals(productive)&&((Map<?,?>)member(m,"lastTownTripAt").get(m)).get(p.getObjectId()).equals(last),"failed town route preserves productive history");
  System.out.println("TOWN SUPPLY CHECKPOINT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
