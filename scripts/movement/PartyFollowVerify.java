import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.location.Location;
import dev.l2j.autobots.Autobot;

/** Live: party member Sandaiver (no scroll, far from its leader) restarted a town walk 2562 times in 30 minutes. */
public class PartyFollowVerify extends IslandRecoveryVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  List<Autobot> bots=new ArrayList<>();
  for(String n:new String[]{"CekaC","Sandaiver"}){Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName",n);check(b!=null,n+" restores");
   Object life=Class.forName("dev.l2j.autobots.population.LifeBehavior").getConstructor(Autobot.class).newInstance(b);invoke(b,"setLife",life);
   suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");bots.add(b);}
  Autobot leader=bots.get(0),member=bots.get(1);Thread.sleep(300);
  warp(member,new Location(35431,52437,-3512));warp(leader,new Location(35531,52437,-3512));
  {net.sf.l2j.gameserver.model.item.instance.ItemInstance soe=member.getInventory().getItemByItemId(736);if(soe!=null)member.getInventory().destroyItem("Verify",soe.getObjectId(),soe.getCount(),member,null);}
  Class<?> kind=Class.forName("dev.l2j.autobots.population.group.PartyKind");Object casual=null;for(Object k:kind.getEnumConstants())if(k.toString().equals("Casual"))casual=k;
  invoke(singleton("population.group.GroupFinder"),"form",leader,bots,casual);if(!member.isInParty()){net.sf.l2j.gameserver.model.group.Party party=new net.sf.l2j.gameserver.model.group.Party(leader,member,net.sf.l2j.gameserver.enums.LootRule.ITEM_LOOTER);}
  check(member.isInParty()&&member.getParty().getLeader()==leader,"member joins the leader's casual party");warp(leader,new Location(14000,47000,net.sf.l2j.gameserver.geoengine.GeoEngine.getInstance().getHeight(14000,47000,-3500)));
  net.sf.l2j.gameserver.model.spawn.L2Spawn sp=new net.sf.l2j.gameserver.model.spawn.L2Spawn(net.sf.l2j.gameserver.data.xml.NpcData.getInstance().getTemplate(20471));
  sp.setLoc(member.getX()+150,member.getY(),member.getZ(),0);sp.setRespawnState(false);net.sf.l2j.gameserver.model.actor.instance.Monster mob=(net.sf.l2j.gameserver.model.actor.instance.Monster)sp.doSpawn(false);mob.setIsInvul(true);mob.addDamageHate(member,0,100);mob.getAI().setIntention(net.sf.l2j.gameserver.enums.IntentionType.ATTACK,member);
  member.setIsInvul(true);
  AtomicReference<Throwable> error=new AtomicReference<>();long began=System.currentTimeMillis();
  while(System.currentTimeMillis()-began<60000){update(member,error);if(error.get()!=null)throw new RuntimeException(error.get());Thread.sleep(300);}
  System.out.println("FOLLOW INFO gap="+(int)Math.hypot(member.getX()-leader.getX(),member.getY()-leader.getY())+" at="+location(member));
  System.out.println("PARTY FOLLOW DONE");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
