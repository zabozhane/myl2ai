import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.player.Experience;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Controlled level-19 profession eligibility; ordinary live NPC aggression, combat and XP. */
public class OfflineProfessionDefenseVerify extends WeaponGradeSuppliesVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object manager=singleton("offlinelife.OfflineFarmManager"),life=singleton("offlinelife.OfflineLife");invoke(member(manager,"travelTask").get(manager),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");gear=singleton("population.economy.Gear");
  Player p=Player.restore(268486152);check(p!=null,"owner restores for profession defense");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();((Map<Integer,Player>)member(manager,"farming").get(manager)).put(p.getObjectId(),p);invoke(manager,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);p.getStat().setLevel((byte)19);p.getStat().setExp(Experience.LEVEL[19]+1000);p.setAccessLevel(0);p.setIsInvul(false);ItemInstance sword=p.getInventory().getItemByItemId(3);if(sword==null)sword=p.getInventory().addItem("Defense equipment fixture",3,1,p,null);if(!sword.isEquipped())invoke(gear,"equip",p,sword);invoke(AutofarmManager.INSTANCE,"prepareOfflineBehavior",p);check(p.getClassId().level()==0&&p.getLevel()==19,"controlled base class is due for first profession");
  List<Monster> rats=new ArrayList<>();for(Object o:World.getInstance().getObjects())if(o instanceof Monster){Monster m=(Monster)o;if(m.getNpcId()==20359&&!m.isDead()&&!m.isInsideZone(ZoneId.TOWN))rats.add(m);}rats.sort(Comparator.comparingDouble(m->Math.hypot(m.getX()+28856,m.getY()-133144)));Monster enemy=null;GeoEngine geo=GeoEngine.getInstance();
  for(Monster m:rats){int x=m.getX()-120,y=m.getY();int z=geo.getHeight(x,y,m.getZ());if(geo.canMoveToTarget(x,y,z,m.getX(),m.getY(),m.getZ())){enemy=m;warp(p,new Location(x,y,z));break;}}check(enemy!=null&&!p.isInsideZone(ZoneId.PEACE),"real Ratman Warrior field is available");p.getAI().setIntention(IntentionType.IDLE);p.setTarget(enemy);double hp=enemy.getCurrentHp();long xp=p.getExp();enemy.addDamageHate(p,0,100);enemy.getAI().setIntention(IntentionType.ATTACK,p);Thread.sleep(500);check(enemy.getTarget()==p&&p.getKnownTypeInRadius(Monster.class,1000).contains(enemy),"real NPC aggression triggers the ordinary under-attack condition");
  long start=System.currentTimeMillis(),deadline=start+8000;while(System.currentTimeMillis()<deadline&&enemy.getCurrentHp()>=hp&&!p.isDead()){Object scheduled=invoke(life,"schedule",p);if((System.currentTimeMillis()-start)/1000%2==0)System.out.println("DEFENSE DIAG scheduled="+scheduled+" purpose="+invoke(life,"purpose",p)+" job="+((Map<?,?>)member(life,"jobs").get(life)).get(p.getObjectId())+" ai="+p.getAI().getDesire().getIntention()+" target="+p.getTarget()+" enemyTarget="+enemy.getTarget()+" hp="+(int)p.getCurrentHp());Thread.sleep(250);}check(enemy.getCurrentHp()<hp,"profession ownership answers the attacker instead of blocking on quest travel");
  if(Arrays.asList(args).contains("--plan-only")){System.out.println("OFFLINE PROFESSION DEFENSE PLAN ALL PASSED");System.exit(0);}
  check("profession".equals(invoke(life,"purpose",p)),"status identifies pending profession ownership");
  deadline=start+120000;while(System.currentTimeMillis()<deadline&&!p.isDead()&&p.getExp()<=xp){invoke(life,"schedule",p);Thread.sleep(250);}check(!p.isDead()&&p.getExp()>xp,"profession defense defeats real prey and earns experience");check(!p.isGM()&&!p.isInvul(),"profession defense uses no GM, invulnerability or HP replacement");System.out.println("PROFESSION DEFENSE INFO xp="+xp+"->"+p.getExp()+" hp="+(int)p.getCurrentHp()+" seconds="+(System.currentTimeMillis()-start)/1000);
  invoke(life,"stop",p);check(!((Map<?,?>)member(life,"jobs").get(life)).containsKey(p.getObjectId())&&"-".equals(invoke(life,"purpose",p)),"owner handoff joins the job and clears profession ownership");System.out.println("OFFLINE PROFESSION DEFENSE ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
