import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import kotlin.Unit;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Isolated ordinary navigation: a real shop visit must leave a way back out. */
public class NpcReturnVerify extends TownUnstuckVerify {
 static void exact(Player p,Location at)throws Exception{warp(p,new Location(at.getX(),at.getY(),at.getZ()-5));}
 static void reversible(Player p,Location from,Object route,String label)throws Exception{
  routeValid(p,from,route,label);GeoEngine geo=GeoEngine.getInstance();
  for(Location at:(List<Location>)invoke(route,"getPoints")){
   check(geo.canMoveToTarget(at.getX(),at.getY(),at.getZ(),from.getX(),from.getY(),from.getZ()),label+" leaves a reverse geodata link");from=at;
  }
 }
 static void walk(Player p,Location goal,Object route)throws Exception{
  AtomicBoolean failed=new AtomicBoolean(false);Object sequence=Class.forName("dev.l2j.autobots.behaviors.sequences.WalkToSequence")
   .getConstructor(Player.class,Location.class,int.class,kotlin.jvm.functions.Function0.class).newInstance(p,goal,20,(kotlin.jvm.functions.Function0<Unit>)()->{failed.set(true);return Unit.INSTANCE;});
  if(route!=null)invoke(sequence,"withNpcRoute",route);
  async(sequence,"definition").get(180,TimeUnit.SECONDS);
  check(!failed.get()&&gap(location(p),goal)<=20,"actual shared walk arrives without resetting its time budget");
 }
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);
  Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  check(World.getInstance().getPlayer("Zenthen")==null,"fixture has no automatic offline startup actions");
  Player p=Player.restore(268486152);check(p!=null,"snapshot restores navigation actor");
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();p.setAccessLevel(0);p.setIsInvul(false);p.getAppearance().setVisible();
  Object nav=singleton("population.economy.NpcTravel");Location entry=new Location(-15303,124111,-3107);Npc harmony=npc("Harmony");
  exact(p,entry);Object route=invoke(nav,"plan",p,harmony,location(p),12000);reversible(p,location(p),route,"Harmony approach");
  if(Arrays.asList(args).contains("--plan-only")){System.out.println("NPC RETURN PLAN ALL PASSED");System.exit(0);}
  long scrolls=p.getInventory().getInventoryItemCount(736,-1);int wallet=p.getAdena();
  check(Boolean.TRUE.equals(suspendValue(nav,"reach",p,harmony,12000)),"normal NPC walker really reaches Harmony");
  check(!(Boolean)invoke(singleton("movement.TownUnstuck"),"isSealed",p),"shop arrival is outside a sealed geodata pocket");
  Object market=singleton("population.economy.Market");kotlin.Pair<?,?> product=(kotlin.Pair<?,?>)invoke(market,"findProduct",p,1060);
  check(product!=null&&invoke(product.getFirst(),"getMerchant")==harmony,"actual shop quote belongs to Harmony");
  int price=(Integer)invoke(market,"priceWithTax",product.getFirst(),product.getSecond());long heals=p.getInventory().getInventoryItemCount(1060,-1);
  check((Boolean)invoke(market,"buyFromNpc",p,product.getFirst(),product.getSecond(),1),"real merchant transaction succeeds after arrival");
  check(p.getAdena()==wallet-price&&p.getInventory().getInventoryItemCount(1060,-1)==heals+1,"purchase uses original funds and exact NPC price");
  route=invoke(nav,"returnableToPoint",p,location(p),entry,12000);reversible(p,location(p),route,"shop departure");walk(p,entry,route);
  check(p.getInventory().getInventoryItemCount(736,-1)==scrolls,"shop visit and departure consume no escape scroll");
  exact(p,new Location(-14024,122584,-3112));walk(p,new Location(-14040,123272,-3112),null);
  exact(p,new Location(-14478,121899,-3035));check(Boolean.TRUE.equals(suspendValue(nav,"reach",p,npcId(30256),12000)),"Bella route really arrives after the recorded intermediate-cell failure");
  for(String name:new String[]{"Papuma","Herbiel","Vollodos"}){
   Npc n=npc(name);warp(p,MapRegionData.getInstance().getLocationToTeleport(n,MapRegionData.TeleportType.TOWN));
   route=invoke(nav,"plan",p,n,location(p),12000);reversible(p,location(p),route,name+" ordinary village approach");
  }
  check(!p.isGM(),"all navigation and the shop transaction use an ordinary character");
  System.out.println("NPC RETURN ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
