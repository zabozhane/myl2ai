import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.*;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.player.*;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.enums.actors.*;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Controlled level, gear, death-memory and NPC positions; ordinary selection, core combat and earned XP. */
public class FallbackTargetWindowVerify extends InventoryBowReadinessVerify {
 static kotlin.ranges.IntRange range(Object spots,Player p)throws Exception{return (kotlin.ranges.IntRange)invoke(spots,"targetLevels",p);}
 static Monster monster(int id,Location at,int dx)throws Exception{
  net.sf.l2j.gameserver.model.spawn.L2Spawn spawn=new net.sf.l2j.gameserver.model.spawn.L2Spawn(NpcData.getInstance().getTemplate(id));spawn.setLoc(at.getX()+dx,at.getY(),at.getZ(),0);spawn.setRespawnState(false);Monster m=(Monster)spawn.doSpawn(false);m.getAI().setIntention(IntentionType.IDLE);check(m.getCurrentHp()>=m.getMaxHp(),"ordinary spawn starts the NPC at full health");return m;
 }
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object fm=singleton("offlinelife.OfflineFarmManager"),manager=singleton("AutobotsManager"),spots=singleton("population.HuntingSpots");invoke(member(fm,"travelTask").get(fm),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");gear=singleton("population.economy.Gear");field();
  Autobot b=new Autobot(IdFactory.getInstance().getNextId(),PlayerData.getInstance().getTemplate(ClassId.ORC_FIGHTER),new Appearance((byte)0,(byte)0,(byte)0,Sex.MALE));b.setName("FallbackTargetPolicy");b.setAccessLevel(0);b.setXYZ(field.getX(),field.getY(),field.getZ());b.getStat().setLevel((byte)13);b.getStat().setExp(Experience.LEVEL[13]+1000);suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);b.abortAttack();b.abortCast();b.stopMove(null);b.setTarget(null);b.setIsInvul(false);Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);Object fitness=member(life,"fitness").get(life),combat=invoke(b,"getCombatBehavior");
  check(range(spots,b).getLast()==14,"normal population novice retains its existing level allowance");
  // Exercise real mode transitions without deliberately killing the actor three times.
  for(int i=0;i<3;i++)invoke(fitness,"onDeath");check(invoke(fitness,"getMode").toString().equals("Farm"),"controlled three-death memory enters ordinary Farm mode");check(range(spots,b).getFirst()==3&&range(spots,b).getLast()==10,"Farm targets the easier hunting level instead of the character's original level");
  if(Arrays.asList(args).contains("--plan-only")){System.out.println("FALLBACK TARGET WINDOW PLAN ALL PASSED");System.exit(0);}
  check(Boolean.TRUE.equals(invoke(spots,"isDangerousFor",b,12)),"guards above the recovery window reject fallback anchors");
  Object cached=Class.forName("dev.l2j.autobots.population.HuntingSpot").getConstructor(long.class,Location.class,List.class,int.class,List.class).newInstance(-987654321L,field,Arrays.asList(8,8,8,8),12,Collections.emptyList());member(life,"spot").set(life,cached);member(life,"spotLevel").set(life,8);Object selected=invoke(life,"currentSpot");check(selected!=cached,"a cached spot with guards above the fallback window is re-evaluated");
  Player owner=Player.restore(268486152);owner.getStat().setLevel((byte)13);Map<Integer,Player> farmers=(Map<Integer,Player>)member(fm,"farming").get(fm);farmers.put(owner.getObjectId(),owner);check(range(spots,owner).getLast()==13,"offline owner keeps its existing novice limit");farmers.remove(owner.getObjectId());check(range(spots,owner).getLast()==16,"manual owner keeps its existing target allowance");
  Monster high=monster(20013,field,70),low=monster(20120,field,160);check(!high.isAggressive()&&!low.isAggressive(),"controlled NPCs do not attack until provoked");check(!NoviceTargetsVerify.worth(b,high)&&NoviceTargetsVerify.worth(b,low),"actual combat predicate accepts easy prey and excludes the nearby ordinary-level enemy");
  high.setTarget(b);check(NoviceTargetsVerify.worth(b,high),"self-defense still accepts an attacker outside the recovery range");high.setTarget(null);
  invoke(b,"setQuestTargets",Collections.singleton(high.getNpcId()));check(NoviceTargetsVerify.worth(b,high),"explicit quest targets retain their separate rules");invoke(b,"setQuestTargets",(Object)null);
  // A group uses its existing common combat policy, rather than one member's solo mode.
  Player partner=Player.restore(268486152);check(partner!=null,"party policy fixture restores");Class<?> pc=Class.forName("net.sf.l2j.gameserver.model.group.Party"),lc=Class.forName("net.sf.l2j.gameserver.enums.LootRule");Object party=pc.getConstructor(Player.class,Player.class,lc).newInstance(b,partner,lc.getEnumConstants()[0]);check(range(spots,b).getLast()==14,"joining a party preserves the established group window");b.setParty(null);partner.setParty(null);
  for(int i=0;i<3;i++)invoke(fitness,"onDeath");check(invoke(fitness,"getMode").toString().equals("NeedsGroup")&&range(spots,b).getLast()==7,"continued failures lower the solo prey window again");
  ItemInstance sword=b.getInventory().getItemByItemId(3);if(sword==null)sword=b.getInventory().addItem("Fallback controlled owned gear",3,1,b,null);invoke(gear,"equip",b,sword);invoke(b,"setQuestTargets",(Object)null);b.setTarget(null);invoke(combat,"setCommittedTarget",(Object)null);b.getAI().setIntention(IntentionType.IDLE);
  // Exercise the ordinary closest-target selector before core combat, with the excluded Dryad closer.
  Object prefs=invoke(combat,"getCombatPreferences");Object closest=Enum.valueOf((Class)Class.forName("dev.l2j.autobots.behaviors.preferences.TargetingPreference"),"Closest");suspendValue(combat,"tryTargetCreatureByTypeInRadius",1000,closest);check(b.getTarget()==low,"ordinary selection skips the closer hard NPC and chooses the reachable easy Wolf");
  double highHp=high.getCurrentHp();long xp=b.getExp(),until=System.currentTimeMillis()+45000;while(System.currentTimeMillis()<until&&!b.isDead()&&!low.isDead()){await(combat,"onUpdate");Thread.sleep(300);}check(low.isDead()&&!b.isDead()&&b.getExp()>xp,"easier ordinary prey is actually killed and grants core experience");System.out.println("FALLBACK EXCLUDED INFO hp="+highHp+"->"+high.getCurrentHp()+" max="+high.getMaxHp()+" target="+high.getTarget());check(high.getCurrentHp()>=highHp&&high.getTarget()!=b,"the excluded enemy remains unprovoked through combat");check(!b.isGM()&&!b.isInvul(),"no GM or invulnerability is used");System.out.println("FALLBACK TARGET INFO xp="+xp+"->"+b.getExp()+" hp="+(int)b.getCurrentHp()+" mode="+invoke(fitness,"getMode"));System.out.println("FALLBACK TARGET WINDOW ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
