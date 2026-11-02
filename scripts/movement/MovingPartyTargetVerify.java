import java.util.*;
import java.util.concurrent.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;
public class MovingPartyTargetVerify extends TownUnstuckVerify {
 @SuppressWarnings({"rawtypes","unchecked"}) public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object off=singleton("offlinelife.OfflineFarmManager");invoke(member(off,"travelTask").get(off),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");Object manager=singleton("AutobotsManager");
  List<Autobot> bots=new ArrayList<>();List<Object> lives=new ArrayList<>();Object hunt=Enum.valueOf((Class)Class.forName("dev.l2j.autobots.population.SessionPlan$Activity"),"Hunt");
  for(String name:new String[]{"TRAMP","Xrukla","furion"}){Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName",name);check(b!=null,"recorded party actor restores: "+name);Location original=location(b);suspendValue(manager,"spawnAutobot",b);((Map<?,?>)invoke(manager,"getActiveBots")).remove(name);invoke(b,"cancelSequence");Thread.sleep(300);b.abortAttack();b.abortCast();b.stopMove(null);b.setTarget(null);b.setAccessLevel(0);b.setIsInvul(false);if(name.equals("TRAMP"))warp(b,new Location(121352,-191528,-3696));else if(gap(original,location(b))>30)warp(b,original);Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);invoke(member(life,"session").get(life),"switchTo",hunt,30,"recorded party path audit");bots.add(b);lives.add(life);System.out.println("PARTY ORIGIN INFO start="+name+" lv="+b.getLevel()+" at="+location(b)+" xp="+b.getExp()+" cash="+b.getAdena()+" scrolls="+b.getInventory().getInventoryItemCount(736,-1));}
  Object kind=Enum.valueOf((Class)Class.forName("dev.l2j.autobots.population.group.PartyKind"),"Casual");invoke(singleton("population.group.GroupFinder"),"form",bots.get(0),bots.subList(1,3),kind);check(bots.get(0).getParty()!=null,"ordinary group finder forms the recorded roster");Object state=invoke(singleton("population.group.PartyPlan"),"state",bots.get(0).getParty());member(state,"gatheringSince").set(state,Long.valueOf(System.currentTimeMillis()-121000));
  CompletableFuture<Object>[] turns=new CompletableFuture[3];Object[] oldWalk=new Object[3];long[] xp=bots.stream().mapToLong(Autobot::getExp).toArray();long began=System.currentTimeMillis(),reported=0,homeAt=0,until=began+720000;boolean verified=false,reunited=false;
  while(System.currentTimeMillis()<until){
   for(int i=0;i<3;i++){
    if(turns[i]!=null&&turns[i].isDone()){turns[i].get();turns[i]=null;}
    if(bots.get(i).isDead())throw new AssertionError("party actor died before continuation: "+bots.get(i).getName());
    if(turns[i]==null)turns[i]=async(bots.get(i),"onUpdate");
    if(i>0&&oldWalk[i]==null){Object active=invoke(bots.get(i),"getActiveSequence");if(active!=null&&active.getClass().getSimpleName().equals("WalkToSequence"))oldWalk[i]=active;}
   }
   if(homeAt==0&&bots.get(0).isInsideZone(net.sf.l2j.gameserver.enums.ZoneId.TOWN)&&!bots.get(0).isTeleporting()){
    homeAt=System.currentTimeMillis();check(oldWalk[1]!=null&&oldWalk[2]!=null,"both followers started actual roads to the old leader position");check(bots.get(0).getInventory().getInventoryItemCount(736,-1)==0,"leader really uses its own scroll to leave the closed field cell");
   }
   if(homeAt>0&&!verified&&System.currentTimeMillis()-homeAt>5000){
    for(int i=1;i<3;i++){check(invoke(bots.get(i),"getActiveSequence")!=oldWalk[i],bots.get(i).getName()+" stops the obsolete road within five seconds of the leader's actual return");Object follow=member(lives.get(i),"party").get(lives.get(i));check(member(follow,"travelFailedAt").getLong(follow)==0,"a changed follow target does not trigger failed-road backoff");}
    verified=true;if(Arrays.asList(args).contains("--retarget-only")){System.out.println("MOVING PARTY TARGET PLAN ALL PASSED");System.exit(0);}
   }
   if(verified&&bots.stream().allMatch(b->b.isInParty()&&b.getParty()==bots.get(0).getParty()&&gap(location(b),location(bots.get(0)))<=1500&&Math.abs(b.getZ()-bots.get(0).getZ())<300))reunited=true;
   boolean earned=true;for(int i=0;i<3;i++)earned&=bots.get(i).getExp()>xp[i];
   if(reunited&&earned&&!bots.get(0).isInsideZone(net.sf.l2j.gameserver.enums.ZoneId.TOWN))break;
   if(System.currentTimeMillis()-reported>20000){reported=System.currentTimeMillis();for(int i=0;i<3;i++){Autobot b=bots.get(i);System.out.println("MOVING PARTY INFO seconds="+(reported-began)/1000+" name="+b.getName()+" at="+location(b)+" xp="+b.getExp()+" party="+b.isInParty()+" sequence="+invoke(b,"getActiveSequence")+" reunited="+reunited);}}
   Thread.sleep(200);
  }
  check(verified&&reunited,"followers physically reunite with their current leader after cancelling obsolete roads");for(int i=0;i<3;i++){check(bots.get(i).getExp()>xp[i],bots.get(i).getName()+" gains ordinary combat experience with the reunited party");check(bots.get(i).isInParty()&&!bots.get(i).isGM()&&!bots.get(i).isInvul(),"group continuation retains normal resources and combat rules");System.out.println("MOVING PARTY INFO xp="+bots.get(i).getName()+" "+xp[i]+"->"+bots.get(i).getExp());}
  System.out.println("MOVING PARTY INFO seconds="+(System.currentTimeMillis()-began)/1000);System.out.println("MOVING PARTY TARGET ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
