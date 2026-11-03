import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/**
 * A no-scroll, no-healing supply walk to town is jumped by an ordinary aggressive monster.
 * Zenthen must defend, then continue the same supply walk instead of hunting on the road,
 * and physically reach town. Position, emptied supplies and the attacker are controlled;
 * no GM, invulnerability, healing or money is given.
 */
public class SupplyWalkDefenseVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"Zenthen restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  warp(p,new Location(-37096,-118374,GeoEngine.getInstance().getHeight(-37096,-118374,-1760)));for(int id:new int[]{736,1060,1061,1539,1540})erase(p,id);
  long began=System.currentTimeMillis();invoke(m,"startTownTrip",p,true,"healing_stock");
  Map<?,?> travels=(Map<?,?>)member(m,"traveling").get(m);Map<?,?> defense;try{defense=(Map<?,?>)member(m,"supplyDefense").get(m);}catch(NoSuchFieldException e){defense=Collections.emptyMap();System.out.println("SUPPLY DEFENSE INFO build has no supply defense state");}Object trip=null;
  long wait=System.currentTimeMillis()+20000;while(System.currentTimeMillis()<wait){trip=travels.get(p.getObjectId());if(trip!=null&&!hasTownAction(m,p))break;Thread.sleep(20);}
  check(trip!=null&&invoke(trip,"getTrip").toString().equals("TO_TOWN"),"no scroll and no healing install a checked supply walk");
  for(int i=0;i<15;i++){invoke(m,"tick");Thread.sleep(300);}
  check(travels.get(p.getObjectId())!=null&&!p.isInsideZone(ZoneId.TOWN),"the walk is under way in the field");
  net.sf.l2j.gameserver.model.spawn.L2Spawn spawn=new net.sf.l2j.gameserver.model.spawn.L2Spawn(NpcData.getInstance().getTemplate(20025));
  spawn.setLoc(p.getX()+120,p.getY(),GeoEngine.getInstance().getHeight(p.getX()+120,p.getY(),p.getZ()),0);spawn.setRespawnState(false);Monster mob=(Monster)spawn.doSpawn(false);
  mob.addDamageHate(p,0,100);mob.getAI().setIntention(IntentionType.ATTACK,p);
  System.out.println("SUPPLY DEFENSE INFO attacker="+mob.getName()+"/"+mob.getLevel()+" at="+location(p)+" hp="+(int)p.getCurrentHp());
  long deadline=System.currentTimeMillis()+120000;boolean defended=false;
  while(System.currentTimeMillis()<deadline&&!p.isDead()&&!mob.isDead()){invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);if(defense.containsKey(p.getObjectId()))defended=true;Thread.sleep(300);}
  check(!p.isDead()&&mob.isDead(),"Zenthen fights off the real attacker");
  System.out.println("SUPPLY DEFENSE INFO attacker dead seconds="+(System.currentTimeMillis()-began)/1000+" defended="+defended+" hp="+(int)p.getCurrentHp());
  Object resumed=null;long resumeBy=System.currentTimeMillis()+20000;
  while(System.currentTimeMillis()<resumeBy&&!p.isDead()){invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);resumed=travels.get(p.getObjectId());if(resumed!=null)break;Thread.sleep(300);}
  check(resumed!=null&&invoke(resumed,"getTrip").toString().equals("TO_TOWN"),"after the fight the same supply walk continues to town");
  check(!AutofarmManager.INSTANCE.isAutoFarming(p),"the fight did not turn into a hunt on the road");
  if(Arrays.asList(args).contains("--resume-only")){System.out.println("SUPPLY WALK DEFENSE RESUME ALL PASSED");System.exit(0);}
  long arriveBy=System.currentTimeMillis()+600000,report=System.currentTimeMillis();boolean arrived=false;
  while(System.currentTimeMillis()<arriveBy&&!p.isDead()){invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);if(p.isInsideZone(ZoneId.TOWN)){arrived=true;break;}
   if(System.currentTimeMillis()-report>=20000){report=System.currentTimeMillis();System.out.println("SUPPLY DEFENSE INFO seconds="+(report-began)/1000+" at="+location(p)+" trip="+travels.get(p.getObjectId())+" farming="+AutofarmManager.INSTANCE.isAutoFarming(p));}Thread.sleep(300);}
  System.out.println("SUPPLY DEFENSE RESULT seconds="+(System.currentTimeMillis()-began)/1000+" at="+location(p)+" arrived="+arrived+" dead="+p.isDead());
  check(arrived&&!p.isDead(),"the resumed supply walk physically reaches town alive");
  check(!p.isGM()&&!p.isInvul(),"no GM or invulnerability");
  System.out.println("SUPPLY WALK DEFENSE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
