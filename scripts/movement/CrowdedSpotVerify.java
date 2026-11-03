import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/**
 * Live death: from the anchor near -6072,40488 offline Zenthen walked to -15583,48512, a nest of 14 aggressive
 * Dark/Lesser Dark Horrors within 1500, and was killed by 11 attackers. Repeated ordinary choices from that anchor
 * must never pick the nest, while ordinary prey stays available. Controlled position only; no GM or gifts.
 */
public class CrowdedSpotVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"Zenthen restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  warp(p,new Location(-6072,40488,GeoEngine.getInstance().getHeight(-6072,40488,-3696)));
  Object spots=singleton("population.HuntingSpots");Location nest=new Location(-15583,48512,-3760);Long nestKey=null;
  for(Object s:(List<?>)invoke(spots,"getSpots"))if(gap((Location)invoke(s,"getLocation"),nest)<300)nestKey=(Long)invoke(s,"getKey");
  check(nestKey!=null,"the live death spot is a known hunting spot");
  int nestPicks=0,picks=0;Map<Long,Integer> seen=new TreeMap<>();
  for(int i=0;i<300;i++){Object s=invoke(spots,"choose",p,Math.max(1,p.getLevel()-2),Collections.emptySet(),null,30000,false,Collections.emptySet());
   if(s==null)continue;picks++;Long k=(Long)invoke(s,"getKey");seen.merge(k,1,Integer::sum);if(k.equals(nestKey))nestPicks++;}
  System.out.println("CROWDED INFO level="+p.getLevel()+" picks="+picks+" nestPicks="+nestPicks+" distinct="+seen.size());
  check(picks>0,"ordinary prey is still chosen around the anchor");
  check(nestPicks==0,"a lone offline hunter is never sent into the aggressive nest");
  check(!p.isGM()&&!p.isInvul(),"no GM or invulnerability");
  System.out.println("CROWDED SPOT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
