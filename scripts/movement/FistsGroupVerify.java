import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;

/** Live Orc village residents: two punching Orc Mystics (lv 7) and Orc Fighters (lv 8) form a casual group. */
public class FistsGroupVerify extends OfflineTownVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  Object gf=singleton("population.group.GroupFinder");List<Autobot> all=new ArrayList<>();
  for(String n:new String[]{"Voviks","BOJIAHD","MustDie","Elodia","Lor"}){
   Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName",n);check(b!=null,n+" restores");
   Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);
   suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");
   warp(b,new Location(-44100+all.size()*60,-114000,net.sf.l2j.gameserver.geoengine.GeoEngine.getInstance().getHeight(-44100+all.size()*60,-114000,-1872)));all.add(b);}
  Thread.sleep(500);
  for(Autobot b:all){invoke(gf,"onSessionStart",b);if(b.getClassId().getId()!=49)invoke(gf,"wantGroup",b);System.out.println("GROUP INFO "+b.getName()+" lv="+b.getLevel()+" class="+b.getClassId()+" looking="+invoke(gf,"isLooking",b));}
  Autobot mystic=all.get(0);check((Boolean)invoke(gf,"isLooking",mystic),"a level 7 mystic without an attack spell looks for a group");
  for(int i=0;i<3&&!mystic.isInParty();i++){invoke(gf,"tick",all);Thread.sleep(1000);}
  for(Autobot b:all)System.out.println("GROUP INFO "+b.getName()+" party="+(b.isInParty()?b.getParty().getMembers().stream().map(x->x.getName()).reduce((a,c)->a+","+c).get():"-"));
  check(mystic.isInParty()&&mystic.getParty().getMembers().stream().anyMatch(x->x.getClassId().getId()==44),"the punching mystic ends up in a party with an orc fighter");
  System.out.println("FISTS GROUP ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
