import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.Autobot;

/** A lone resident with Zenthen's level and position prefers ordinary prey over the live aggressive nest. */
public class PopulationCrowdedVerify extends OfflineTownVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object spots=singleton("population.HuntingSpots");
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Malibu");check(b!=null,"a recorded resident restores");
  b.getStat().setLevel((byte)20);b.setXYZInvisible(-6072,40488,GeoEngine.getInstance().getHeight(-6072,40488,-3696));check(!b.isInParty(),"controlled lone resident at Zenthen's live anchor, level 20");
  Location nest=new Location(-15583,48512,-3760);Long nestKey=null;for(Object s:(List<?>)invoke(spots,"getSpots"))if(gap((Location)invoke(s,"getLocation"),nest)<300)nestKey=(Long)invoke(s,"getKey");
  int nestPicks=0,picks=0;for(int i=0;i<300;i++){Object s=invoke(spots,"choose",b,18,Collections.emptySet(),null,30000,false,Collections.emptySet());if(s==null)continue;picks++;if(invoke(s,"getKey").equals(nestKey))nestPicks++;}
  System.out.println("POP CROWDED INFO picks="+picks+" nestPicks="+nestPicks);
  check(picks>0,"the resident still finds prey");check(nestPicks==0,"the resident avoids the aggressive nest while other prey exists");
  System.out.println("POPULATION CROWDED ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
