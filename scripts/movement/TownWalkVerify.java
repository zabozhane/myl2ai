import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** No-scroll supply return beyond the former 6000-unit cut-off; no gifted cash or GM protection. */
public class TownWalkVerify extends OfflineTownVerify{
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException("Disposable world required");
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=World.getInstance().getPlayer("Zenthen");check(p!=null,"snapshot restores supply-return fixture");invoke(AutofarmManager.INSTANCE,"quiesce",p);
  @SuppressWarnings("unchecked") Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);farmers.entrySet().removeIf(e->e.getKey()!=p.getObjectId());
  ((Map<?,?>)member(m,"traveling").get(m)).clear();p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
  warp(p,new Location(-37096,-118374,GeoEngine.getInstance().getHeight(-37096,-118374,-1760)));
  for(int item:new int[]{736,1060,1061,1539,1540})erase(p,item);
  int money=p.getAdena();check(money>=1490,"fixture owns its saved shopping budget without creating money");
  long productive=System.currentTimeMillis()-660000,lastTown=System.currentTimeMillis()-3600000;
  stamp(m,"productiveSince",p,productive);stamp(m,"lastTownTripAt",p,lastTown);
  Location origin=location(p);long began=System.currentTimeMillis();invoke(m,"startTownTrip",p,true,"healing_stock");
  Map<?,?> travels=(Map<?,?>)member(m,"traveling").get(m);Object trip=null;
  while(System.currentTimeMillis()-began<20000){trip=travels.get(p.getObjectId());if(trip!=null||!hasTownAction(m,p))break;Thread.sleep(50);}
  check(trip!=null&&invoke(trip,"getTrip").toString().equals("TO_TOWN"),"missing SoE installs a checked town walk instead of an immediate refusal");
  Location goal=new Location((Integer)invoke(trip,"getTargetX"),(Integer)invoke(trip,"getTargetY"),(Integer)invoke(trip,"getTargetZ"));
  check(gap(origin,goal)>6000,"real supply trip exceeds the old direct-distance limit");
  check(((Map<?,?>)member(m,"productiveSince").get(m)).get(p.getObjectId()).equals(productive)&&((Map<?,?>)member(m,"lastTownTripAt").get(m)).get(p.getObjectId()).equals(lastTown),"starting a road does not erase productive time or count as a completed town visit");
  @SuppressWarnings("unchecked") List<Location> route=(List<Location>)invoke(trip,"getRoute");Location previous=origin;double routeLength=0;
  check(!route.isEmpty(),"town walk has validated points before its first movement");
  for(Location point:route){check(GeoEngine.getInstance().canMoveToTarget(previous.getX(),previous.getY(),previous.getZ(),point.getX(),point.getY(),point.getZ()),"town route respects walls and floors");check((Boolean)invoke(singleton("population.RouteSafety"),"safeSegment",p,previous,point),"town route respects hazards");routeLength+=gap(previous,point);previous=point;}
  long deadline=System.currentTimeMillis()+Math.min(1080000,(long)(routeLength/Math.max(30,p.getMoveSpeed())*2000+195000)),reported=0;boolean reached=false,complete=false;
  while(System.currentTimeMillis()<deadline){
   invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
   if(p.isDead())throw new AssertionError("supply return died en route");
   if(p.isInsideZone(ZoneId.TOWN))reached=true;
   Object current=travels.get(p.getObjectId());
   if(reached&&p.getInventory().getInventoryItemCount(1060,-1)>0&&current!=null&&invoke(current,"getTrip").toString().equals("BACK_TO_SPOT")){complete=true;break;}
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();System.out.println("TOWN WALK INFO at="+location(p)+" reached="+reached+" cash="+p.getAdena()+" potions="+p.getInventory().getInventoryItemCount(1060,-1)+" trip="+(current==null?"none":invoke(current,"getTrip")+":"+invoke(current,"getRouteIndex")));}
   Thread.sleep(300);
  }
  check(complete,"ordinary no-scroll return actually reaches town, buys supplies and schedules the return to its spot");
  check(!p.isGM()&&p.getInventory().getInventoryItemCount(736,-1)==1,"newbie buys one real escape scroll without GM protection");
  check(!((Map<?,?>)member(m,"productiveSince").get(m)).containsKey(p.getObjectId())&&((Long)((Map<?,?>)member(m,"lastTownTripAt").get(m)).get(p.getObjectId()))>=began,"productive clock resets only after the actual town visit");
  // A failed route gets a finite retry and must not pretend that city chores took place.
  Object completedTrip=trip;((Map<?,?>)member(m,"traveling").get(m)).clear();p.stopMove(null);
  stamp(m,"productiveSince",p,productive);stamp(m,"lastTownTripAt",p,lastTown);invoke(m,"onStuck",p,completedTrip);
  check(((Long)((Map<?,?>)member(m,"townRetryAfter").get(m)).get(p.getObjectId()))>System.currentTimeMillis(),"failed town route schedules a finite retry");
  check(((Map<?,?>)member(m,"productiveSince").get(m)).get(p.getObjectId()).equals(productive)&&((Map<?,?>)member(m,"lastTownTripAt").get(m)).get(p.getObjectId()).equals(lastTown),"failed town route preserves productive history");
  System.out.println("TOWN WALK ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
