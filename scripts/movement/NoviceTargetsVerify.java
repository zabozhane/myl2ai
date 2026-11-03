import java.util.*;
import java.lang.reflect.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Offline novice planning and actual combat must share the intentionally safer level window. */
public class NoviceTargetsVerify extends OfflineTownVerify {
 static boolean worth(Player p,Creature target)throws Exception{
  Method f=Class.forName("dev.l2j.autobots.utils.AutobotHelpersKt").getDeclaredMethod("isWorthTargeting",Creature.class,Creature.class);f.setAccessible(true);return (Boolean)f.invoke(null,p,target);
 }
 static void policy(Object m,Player p,Map<Integer,Player> farmers)throws Exception{
  Object spots=singleton("population.HuntingSpots");kotlin.ranges.IntRange window=(kotlin.ranges.IntRange)invoke(spots,"targetLevels",p);check(window.getLast()==p.getLevel(),"offline novice anchors retain the level-2 spot selector's own-level cap");
  check(Boolean.TRUE.equals(invoke(spots,"isDangerousFor",p,17)),"level-17 aggression cannot guard ordinary level-13 hunting anchors");
  Monster high=(Monster)npcId(20515);check(high.getLevel()>p.getLevel(),"recorded skeleton is above the novice's level");check(!worth(p,high),"real combat predicate rejects an unprovoked higher-level skeleton");
  net.sf.l2j.gameserver.model.WorldObject remembered=high.getTarget();high.setTarget(p);check(worth(p,high),"existing self-defense still permits an attacker above the window");high.setTarget(remembered);
  farmers.remove(p.getObjectId());window=(kotlin.ranges.IntRange)invoke(spots,"targetLevels",p);check(window.getLast()==p.getLevel()+3&&worth(p,high),"active owner's existing level window is preserved");farmers.put(p.getObjectId(),p);
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Weapons");check(b!=null,"other novice population class restores");window=(kotlin.ranges.IntRange)invoke(spots,"targetLevels",b);check(window.getLast()==b.getLevel()+1,"population novice's existing one-level allowance is preserved");
  Player archer=Player.restore(268491948);check(archer!=null&&archer.getLevel()<20,"second offline novice class restores");farmers.put(archer.getObjectId(),archer);window=(kotlin.ranges.IntRange)invoke(spots,"targetLevels",archer);check(window.getLast()==archer.getLevel(),"same offline limit applies to another race/class");farmers.remove(archer.getObjectId());
 }
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  check(World.getInstance().getPlayer("Zenthen")==null,"controlled startup leaves the recorded actor untouched");Player p=Player.restore(268486152);check(p!=null&&p.getLevel()==13,"real level-13 novice restores");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();p.setAccessLevel(0);p.setIsInvul(false);
  Map<Integer,Player> farmers=(Map<Integer,Player>)member(m,"farming").get(m);farmers.put(p.getObjectId(),p);invoke(m,"openJournal",p);policy(m,p,farmers);
  if(args.length>0&&args[0].equals("--policy-only")){System.out.println("NOVICE TARGET POLICY ALL PASSED");System.exit(0);}
  long xp=p.getExp(),started=System.currentTimeMillis(),reported=started,deadline=started+900000;Location origin=location(p),goal=null;boolean departed=false,arrived=false;Set<Integer> defenders=new HashSet<>();
  for(String field:new String[]{"lessonCheckedAt","equipmentCheckedAt"})((Map<Integer,Long>)member(m,field).get(m)).put(p.getObjectId(),started+3600000);
  System.out.println("NOVICE TARGET INFO origin="+origin+" cash="+p.getAdena()+" healing="+invoke(singleton("population.economy.Healing"),"count",p)+" weapon="+p.getActiveWeaponItem().getItemId());invoke(m,"goHunting",p);
  while(System.currentTimeMillis()<deadline){
   invoke(m,"tick");Object travel=((Map<?,?>)member(m,"traveling").get(m)).get(p.getObjectId());
   if(travel!=null&&invoke(travel,"getTrip").toString().equals("TO_SPOT"))goal=new Location((Integer)invoke(travel,"getTargetX"),(Integer)invoke(travel,"getTargetY"),(Integer)invoke(travel,"getTargetZ"));
   if(gap(origin,location(p))>=100)departed=true;
   if(goal!=null&&gap(location(p),goal)<=650&&AutofarmManager.INSTANCE.isAutoFarming(p))arrived=true;
   for(Monster attacker:p.getKnownTypeInRadius(Monster.class,2000))if(!attacker.isDead()&&attacker.getTarget()==p)defenders.add(attacker.getObjectId());
   if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
   if(p.isDead())throw new AssertionError("novice died during real selection/arrival/combat");
   if(p.getTarget() instanceof Monster){Monster target=(Monster)p.getTarget();if(!target.isDead()&&target.getLevel()>p.getLevel()&&target.getTarget()!=p&&!defenders.contains(target.getObjectId()))throw new AssertionError("voluntary combat escaped the novice level window");}
   if(arrived&&p.getExp()-xp>=1000)break;
   if(System.currentTimeMillis()-reported>=20000){reported=System.currentTimeMillis();System.out.println("NOVICE TARGET INFO seconds="+(reported-started)/1000+" at="+location(p)+" goal="+goal+" xp="+p.getExp()+" hp="+(int)p.getCurrentHp()+" target="+p.getTarget()+" arrived="+arrived);}
   Thread.sleep(500);
  }
  check(departed&&arrived,"novice physically leaves the recorded position and arrives at its chosen hunting goal");check(p.getExp()-xp>=1000,"real combat gains experience under the same novice window");check(!p.isGM()&&!p.isInvul(),"selection/arrival/combat use ordinary rules");System.out.println("NOVICE TARGET INFO xp="+xp+"->"+p.getExp()+" seconds="+(System.currentTimeMillis()-started)/1000);System.out.println("NOVICE TARGETS ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
