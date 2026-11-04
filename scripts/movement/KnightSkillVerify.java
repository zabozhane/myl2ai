import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Live offline Knight HawkEye hunts at its saved spot with ordinary autofarm: Power Strike must be cast on monsters. */
public class KnightSkillVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268491948);check(p!=null&&p.getClassId().getId()==4,"Knight HawkEye restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);
  check(p.getSkill(3)!=null,"Power Strike is learned");
  AutofarmManager.INSTANCE.startFarm(p);long xp=p.getExp(),began=System.currentTimeMillis();int casts=0;Set<String> seen=new TreeSet<>();
  while(System.currentTimeMillis()-began<90000&&!p.isDead()&&casts<3){if(AutofarmManager.INSTANCE.isAutoFarming(p))suspendValue(AutofarmManager.INSTANCE,"onUpdate",p);
   if(p.isCastingNow()&&p.getLastSkillCast()!=null){String id=""+p.getLastSkillCast().getId();if(seen.add(id+"@"+(System.currentTimeMillis()/2000)))if(p.getLastSkillCast().getId()==3)casts++;}Thread.sleep(200);}
  System.out.println("KNIGHT INFO seconds="+(System.currentTimeMillis()-began)/1000+" powerStrikeCasts="+casts+" skills="+seen+" xp="+xp+"->"+p.getExp()+" dead="+p.isDead());
  check(casts>0,"the knight casts Power Strike on monsters");check(!p.isDead(),"alive");
  System.out.println("KNIGHT SKILLS ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
