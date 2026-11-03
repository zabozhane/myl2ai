import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import dev.l2j.autobots.autofarm.AutofarmManager;

/**
 * Live idle Zenthen origin inside the Dark Elf Village (12056,16568,-4584), lv 20, 219 adena,
 * no affordable ride. The ordinary offline hunt must plan a checked walk out of the underground
 * village, physically leave it and gain experience. No warp, GM, invulnerability or gifts.
 */
public class CaveExitWalkVerify extends GatekeeperVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"live idle snapshot restores Zenthen");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  Location origin=location(p);long xp=p.getExp();int cash=p.getAdena();
  check(gap(origin,new Location(12056,16568,-4584))<1&&p.isInsideZone(ZoneId.TOWN),"exact recorded village origin, not a controlled position");
  check(cash<300,"wallet is too small for any gatekeeper ride");
  Map<?,?> travels=(Map<?,?>)member(m,"traveling").get(m);long began=System.currentTimeMillis();
  invoke(m,"goHunting",p);Object trip=travels.get(p.getObjectId());
  System.out.println("CAVE EXIT INFO planMs="+(System.currentTimeMillis()-began)+" trip="+trip);
  check(trip!=null&&invoke(trip,"getTrip").toString().equals("TO_SPOT"),"ordinary hunt plans a checked walk out of the underground village");
  List<Location> route=(List<Location>)invoke(trip,"getRoute");Location anchor=route.get(route.size()-1);
  check(route.size()>2,"the walk uses validated waypoints, not one blind move");
  System.out.println("CAVE EXIT INFO anchor="+anchor+" points="+route.size());
  if(Arrays.asList(args).contains("--plan-only")){System.out.println("CAVE EXIT PLAN ALL PASSED");System.exit(0);}
  long deadline=System.currentTimeMillis()+1_200_000,report=began;boolean left=false,arrived=false,gained=false;
  while(System.currentTimeMillis()<deadline&&!p.isDead()){
   invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
   if(!p.isInsideZone(ZoneId.TOWN))left=true;if(gap(location(p),anchor)<=1500)arrived=true;
   if(left&&p.getExp()>xp){gained=true;break;}
   if(System.currentTimeMillis()-report>=20000){report=System.currentTimeMillis();System.out.println("CAVE EXIT INFO seconds="+(report-began)/1000+" at="+location(p)+" town="+p.isInsideZone(ZoneId.TOWN)+" trip="+travels.get(p.getObjectId())+" hp="+(int)p.getCurrentHp()+" xp="+p.getExp());}
   Thread.sleep(300);}
  System.out.println("CAVE EXIT RESULT seconds="+(System.currentTimeMillis()-began)/1000+" at="+location(p)+" left="+left+" arrived="+arrived+" dead="+p.isDead()+" xp="+xp+"->"+p.getExp()+" cash="+cash+"->"+p.getAdena());
  check(!p.isDead(),"the walk and the first fights do not end in death");
  check(left&&gained,"Zenthen physically leaves the village and gains experience in an ordinary fight");
  check(!p.isGM()&&!p.isInvul(),"no GM or invulnerability");
  System.out.println("CAVE EXIT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
