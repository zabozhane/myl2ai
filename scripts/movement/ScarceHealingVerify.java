import java.util.*;
import kotlin.Pair;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Controlled field fixture: the real small wallet must not fund a repeated one-potion commute. */
public class ScarceHealingVerify extends OfflineTownVerify {
 static void rules()throws Exception{
  Object policy=singleton("movement.SupplyBudget");
  check(Boolean.FALSE.equals(invoke(policy,"worthRestocking",304,1,108)),"recorded wallet cannot justify a two-potion commute");
  check(Boolean.FALSE.equals(invoke(policy,"worthRestocking",971,1,108)),"one adena below normal stock waits");
  check(Boolean.TRUE.equals(invoke(policy,"worthRestocking",972,1,108)),"normal stock can be funded at the exact price boundary");
  check(Boolean.TRUE.equals(invoke(policy,"worthRestocking",1080,0,108)),"empty bag with a funded normal purchase returns to town");
  check(Boolean.FALSE.equals(invoke(policy,"worthRestocking",Integer.MAX_VALUE,3,108)),"an adequate carried reserve does not trigger an immediate trip");
  check(Boolean.FALSE.equals(invoke(policy,"worthRestocking",0,0,108)),"empty wallet cannot promise supplies");
  check(Boolean.FALSE.equals(invoke(policy,"worthRestocking",Integer.MAX_VALUE,0,Integer.MAX_VALUE)),"large prices do not overflow into an affordable trip");
 }
 @SuppressWarnings("unchecked") static Map<Integer,Long> times(Object manager,String name)throws Exception{return (Map<Integer,Long>)member(manager,name).get(manager);}
 public static void main(String[] args){try{
  if(args.length>0&&args[0].equals("--rules-only")){rules();System.out.println("SCARCE HEALING RULES ALL PASSED");return;}
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");for(Object job:((Map<?,?>)member(m,"townJobs").get(m)).values())invoke(job,"cancel");Thread.sleep(300);((Map<?,?>)member(m,"townJobs").get(m)).clear();invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  check(World.getInstance().getPlayer("Zenthen")==null,"controlled startup suppresses unrelated movement");Player p=Player.restore(268486152);check(p!=null,"snapshot restores the actual short-hunt actor");p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();p.setAccessLevel(0);p.setIsInvul(false);((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);((Map<?,?>)member(m,"farming").get(m)).entrySet().removeIf(e->!((Map.Entry<?,?>)e).getKey().equals(p.getObjectId()));invoke(m,"clearTravel",p.getObjectId(),null);
  // Only position is the controlled fixture: the recorded real hunting field, not a navigation test.
  warp(p,new Location(-23689,142316,-3768));check(!p.isGM()&&!p.isInvul(),"ordinary field actor has no GM protection");
  Object healing=singleton("population.economy.Healing");long carried=((Integer)invoke(healing,"count",p)).longValue();System.out.println("SCARCE HEALING INFO initial healing="+carried+" cash="+p.getAdena()+" xp="+p.getExp());check(carried==3&&p.getAdena()==88,"real snapshot supplies/wallet are preserved without gifts");
  long now=System.currentTimeMillis(),xp=p.getExp();times(m,"lastTownTripAt").put(p.getObjectId(),now);times(m,"productiveSince").put(p.getObjectId(),now-60000);times(m,"lessonCheckedAt").put(p.getObjectId(),now+3600000);times(m,"equipmentCheckedAt").put(p.getObjectId(),now+3600000);times(m,"townCheckedAt").put(p.getObjectId(),now-60000);times(m,"townRetryAfter").remove(p.getObjectId());
  invoke(m,"startFarmLogged",p,"scarce healing field verification");invoke(m,"tick");check(!((Map<?,?>)member(m,"townJobs").get(m)).containsKey(p.getObjectId())&&!((Map<?,?>)member(m,"traveling").get(m)).containsKey(p.getObjectId()),"recorded departure stock starts ordinary hunting");
  if(args.length>0&&args[0].equals("--decision-only")){System.out.println("SCARCE HEALING DECISION ALL PASSED");System.exit(0);}
  boolean rested=false,regenerated=false;double sittingHp=-1;long deadline=now+540000,reported=now;long minimum=now+180000;
  while(System.currentTimeMillis()<deadline){
   invoke(m,"tick");if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);Thread.sleep(500);
   if(p.isDead())throw new AssertionError("ordinary actor died during scarce-healing field cycle");
   if(p.isSitting()){rested=true;if(sittingHp>=0&&p.getCurrentHp()>sittingHp+1)regenerated=true;sittingHp=p.getCurrentHp();}else sittingHp=-1;
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();System.out.println("SCARCE HEALING INFO seconds="+(reported-now)/1000+" xp="+p.getExp()+" hp="+(int)p.getCurrentHp()+" healing="+invoke(healing,"count",p)+" cash="+p.getAdena()+" sitting="+p.isSitting()+" restSeen="+rested+" regen="+regenerated);}
   if(((Map<?,?>)member(m,"townJobs").get(m)).containsKey(p.getObjectId())||((Map<?,?>)member(m,"traveling").get(m)).containsKey(p.getObjectId()))throw new AssertionError("scarce stock starts a short commute");
   if(System.currentTimeMillis()>=minimum&&p.getExp()-xp>=2000&&rested&&regenerated)break;
  }
  check(p.getExp()-xp>=2000,"several real fights gain experience with the recorded small wallet");check(rested&&regenerated,"actor really sits and restores health between fights");check(!p.isGM()&&!p.isInvul(),"field cycle retains ordinary combat rules");System.out.println("SCARCE HEALING INFO xp="+xp+"->"+p.getExp()+" elapsed="+(System.currentTimeMillis()-now)/1000);System.out.println("SCARCE HEALING ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
