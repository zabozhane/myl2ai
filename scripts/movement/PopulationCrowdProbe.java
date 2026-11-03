import java.util.*;
import net.sf.l2j.L2DatabaseFactory;
import net.sf.l2j.gameserver.GameServer;
import dev.l2j.autobots.Autobot;

/** For every recorded population member: is there still ordinary prey within 20000 (and anywhere) at its saved position? */
public class PopulationCrowdProbe extends GatekeeperVerify {
 public static void main(String[] a){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object spots=singleton("population.HuntingSpots");Object dao=singleton("dao.AutobotsDao");
  List<String> names=new ArrayList<>();try(java.sql.Connection c=L2DatabaseFactory.getInstance().getConnection();java.sql.ResultSet r=c.createStatement().executeQuery("SELECT char_name FROM autobots ORDER BY char_name")){while(r.next())names.add(r.getString(1));}
  int near=0,any=0,total=0;
  for(String n:names){Autobot b;try{b=(Autobot)invoke(dao,"loadByName",n);}catch(Throwable t){continue;}if(b==null)continue;total++;
   int lvl=b.getLevel();boolean hasNear=false,hasAny=false;
   for(int i=0;i<5&&!hasNear;i++)hasNear=invoke(spots,"choose",b,lvl,Collections.emptySet(),null,20000,false,Collections.emptySet())!=null;
   hasAny=invoke(spots,"choose",b,lvl,Collections.emptySet(),null,Integer.MAX_VALUE,false,Collections.emptySet())!=null;
   if(hasNear)near++;if(hasAny)any++;
   System.out.println("POPCROWD "+n+" lv="+lvl+" near="+hasNear+" any="+hasAny);}
  System.out.println("POPCROWD SUMMARY total="+total+" near="+near+" any="+any);
  System.out.println("POPCROWD PROBE DONE");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
