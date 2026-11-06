import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;

/**
 * Live 06.11 after the wipe. Crowd: RedLock (Orc, lv 5) sat at the orc village spot -49400,-115600 among other
 * newbies and struck off all 15 spots around it without a step ("no non-excluded hunting spot ... excluded=15"),
 * walking home and back without exp. Prey: kima (Orc Fighter 4) died 22 times to Goblin Grave Robbers and Elder
 * Prairie Keltir, AianamiRei to pairs of Kaboo Orcs: none of them aggressive, so nothing was remembered.
 * Phase 1 runs ordinary updates of RedLock among eight real residents; phase 2 reports controlled deaths from real
 * spawned monsters. No GM, no gifts.
 */
public class NoviceCrowdPreyVerify extends IslandRecoveryVerify {
 static Autobot resident(String name,boolean live)throws Exception{
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName",name);check(b!=null,"snapshot restores "+name);
  if(live){Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);}
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());
  b.setAccessLevel(0);b.setIsInvul(false);return b;
 }
 static Monster spawn(int npc,Location at)throws Exception{
  net.sf.l2j.gameserver.model.spawn.L2Spawn s=new net.sf.l2j.gameserver.model.spawn.L2Spawn(NpcData.getInstance().getTemplate(npc));
  s.setLoc(at.getX(),at.getY(),GeoEngine.getInstance().getHeight(at.getX(),at.getY(),at.getZ()),0);s.setRespawnState(false);return (Monster)s.doSpawn(false);
 }
 static boolean worth(Autobot b,Monster m)throws Exception{
  Method w=Class.forName("dev.l2j.autobots.utils.AutobotHelpersKt").getDeclaredMethod("isWorthTargeting",Creature.class,Creature.class);w.setAccessible(true);return (Boolean)w.invoke(null,b,m);
 }
 static int size(Object life,String field)throws Exception{return ((Map<?,?>)member(life,field).get(life)).size();}
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  Object spots=singleton("population.HuntingSpots");

  // Phase 1: the crowd is counted at the spot, not around a resting bot.
  Location village=new Location(-49400,-115600,-160);
  Autobot red=resident("RedLock",true);Object life=invoke(red,"getLife");warp(red,village);
  int n=0;for(String name:new String[]{"Man1aK","Malkin","DragonHunter","LapaTb","YnItAr","MrKastet4ik","MegaFight","PoBeDa"}){
   Autobot o=resident(name,false);warp(o,new Location(village.getX()+(n%3-1)*150,village.getY()+(n/3-1)*150,village.getZ()));o.sitDown();n++;}
  long exp=red.getExp();int maxBad=0,maxCrowded=0,deaths=0;boolean wasDead=false,left=false;double far=0;
  AtomicReference<Throwable> error=new AtomicReference<>();long began=System.currentTimeMillis(),reported=0;
  while(System.currentTimeMillis()-began<240000){update(red,error);if(error.get()!=null)throw new RuntimeException(error.get());
   maxBad=Math.max(maxBad,size(life,"badSpots"));maxCrowded=Math.max(maxCrowded,size(life,"crowdedSpots"));
   far=Math.max(far,gap(location(red),village));if(far>1500)left=true;
   if(red.isDead()&&!wasDead)deaths++;wasDead=red.isDead();if(red.isDead())red.doRevive();
   if(System.currentTimeMillis()-reported>30000){reported=System.currentTimeMillis();System.out.println("CROWD INFO seconds="+(reported-began)/1000+" at="+location(red)+" exp="+red.getExp()+
     " bad="+size(life,"badSpots")+" crowded="+size(life,"crowdedSpots")+" spot="+member(life,"spot").get(life));}
   Thread.sleep(300);}
  System.out.println("CROWD RESULT exp="+exp+"->"+red.getExp()+" maxBad="+maxBad+" maxCrowded="+maxCrowded+" farthest="+(int)far+" deaths="+deaths);
  check(maxBad<9,"the village crowd no longer strikes off every spot around it (live: 9 at once, then 15)");
  check(left||red.getExp()>exp,"RedLock walks off to hunt or hunts among the others");
  check(red.getExp()>exp,"RedLock gains exp");

  // Phase 2: monsters that are not aggressive but keep killing the same resident.
  Autobot kima=resident("kima",true);Object kl=invoke(kima,"getLife");
  Location graves=new Location(-37900,-114050,-1968);warp(kima,graves);
  Monster g1=spawn(20319,new Location(graves.getX()+80,graves.getY(),graves.getZ())),g2=spawn(20319,new Location(graves.getX()-80,graves.getY(),graves.getZ()));
  check(!g1.isAggressive(),"Goblin Grave Robber is not aggressive");
  check(worth(kima,g1)||g1.getLevel()>kima.getLevel()+1,"before any death the robber is ordinary prey (or out of the level window)");
  g1.setTarget(kima);g2.setTarget(kima);invoke(kl,"onDeath",g1);
  Set<Long> graveSpots=DeadlySpotVerify.keysWith(spots,graves,20319,2500);Set<?> excluded=(Set<?>)invoke(kl,"getExcludedSpots");
  System.out.println("PREY INFO graveSpots="+graveSpots+" excluded="+excluded);
  check((Boolean)invoke(kl,"avoidsPrey",20319),"a death to a pair of robbers makes them no prey");
  check(excluded.containsAll(graveSpots),"the robbers' spots around the place of death are left");
  g1.setTarget(null);g2.setTarget(null);
  check(!worth(kima,g1),"a robber standing idle is not picked");
  g1.setTarget(kima);check(worth(kima,g1),"a robber attacking kima is still fought back");
  g1.deleteMe();g2.deleteMe();

  Location field=new Location(-48000,-112600,-240);warp(kima,field);
  Monster k=spawn(20538,new Location(field.getX()+100,field.getY(),field.getZ()));check(!k.isAggressive(),"Elder Prairie Keltir is not aggressive");
  k.setTarget(kima);invoke(kl,"onDeath",k);
  check(!(Boolean)invoke(kl,"avoidsPrey",20538),"one death to a single keltir is not enough");
  invoke(kl,"onDeath",k);
  check((Boolean)invoke(kl,"avoidsPrey",20538),"a second death to it within the hour is");
  k.setTarget(null);check(!worth(kima,k),"an idle keltir is no longer pulled");
  k.deleteMe();
  Object next=invoke(spots,"choose",kima,kima.getLevel(),invoke(kl,"getExcludedSpots"),null,15000,false,Collections.emptySet());
  System.out.println("PREY INFO next="+next);check(next!=null,"kima still finds a hunting spot");
  check(!kima.isGM()&&!kima.isInvul()&&!red.isGM(),"no GM or invulnerability");
  System.out.println("NOVICE CROWD PREY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
