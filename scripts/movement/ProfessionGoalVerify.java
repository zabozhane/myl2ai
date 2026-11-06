import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Owner's request 2026-11-06: `.autolife class destroyer` makes the life take Orc Raider (then Destroyer), not a random branch. */
public class ProfessionGoalVerify extends OfflineTownVerify {
 static Object path(Player p)throws Exception{Object r=Class.forName("dev.l2j.autobots.population.quests.QuestRunner").getConstructor(Player.class).newInstance(p);Object path=invoke(r,"currentPath");return path==null?null:invoke(path,"getTo");}
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Object cmd=singleton("offlinelife.AutoLifeCommandHandler");
  Player p=Player.restore(268486152);check(p.getClassId()==ClassId.ORC_FIGHTER,"Zenthen is an Orc Fighter");
  Set<Object> random=new HashSet<>();for(int i=0;i<40;i++)random.add(path(p));System.out.println("GOAL INFO without a goal: "+random);
  check(random.size()==2,"without a goal the branch is random (Raider or Monk)");
  check((Boolean)invoke(cmd,"onChat",p,".autolife class destroyer"),".autolife class is a command");
  for(int i=0;i<20;i++)check(path(p)==ClassId.ORC_RAIDER,i==0?"goal Destroyer: the first profession is always Orc Raider":"same");
  Player again=Player.restore(268486152);check(again.getMemos().getString("autolife_class",null).equals("DESTROYER")&&path(again)==ClassId.ORC_RAIDER,"the goal survives a fresh restore from the database");
  invoke(cmd,"onChat",p,".autolife class monk");for(int i=0;i<20;i++)check(path(p)==ClassId.MONK,i==0?"goal Monk: the first profession is Orc Monk":"same");
  invoke(cmd,"onChat",p,".autolife class titan");check(p.getMemos().getString("autolife_class",null).equals("MONK"),"a third profession is not accepted as a goal");
  invoke(cmd,"onChat",p,".autolife class none");check(p.getMemos().getString("autolife_class",null)==null,"none clears the goal");
  System.out.println("PROFESSION GOAL ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
