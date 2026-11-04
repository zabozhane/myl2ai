import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import dev.l2j.autobots.Autobot;

/** Live resident Orc Mystics (lv 7, a few hundred adena, Training Gloves): one town visit buys Amulet: Life Drain and learns it. */
public class PopulationAttackBookVerify extends OfflineTownVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");
  int learnedCount=0;
  for(String name:new String[]{"ALESKA","RERW"}){
   Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName",name);check(b!=null&&b.getClassId().getId()==49,name+" restores as Orc Mystic");
   suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);b.setAccessLevel(0);b.setIsInvul(false);
   warp(b,MapRegionData.getInstance().getLocationToTeleport(b,MapRegionData.TeleportType.TOWN));
   int cash=b.getAdena(),sp=b.getSp();boolean before=b.getSkill(1090)!=null;
   suspendValue(singleton("population.skills.SkillLearner"),"learn",b);
   boolean after=b.getSkill(1090)!=null;if(after)learnedCount++;
   System.out.println("ATTACK BOOK INFO "+name+" lv="+b.getLevel()+" cash="+cash+"->"+b.getAdena()+" sp="+sp+"->"+b.getSp()+" lifeDrain="+before+"->"+after);
   check(!before,name+" did not know Life Drain");
   List<?> lessons=(List<?>)invoke(singleton("population.skills.SkillLearner"),"lessons",b);StringBuilder ids=new StringBuilder();for(Object n:lessons)ids.append(invoke(n,"getId")).append(' ');System.out.println("ATTACK BOOK INFO "+name+" lessons="+ids);
   if(!after)check(b.getSp()==sp,name+" keeps its SP for the attack spell instead of other lessons");
   Object first=lessons.get(0);check(((Integer)invoke(first,"getId"))==1090,name+" saves SP for Life Drain before masteries");
   Object budget=invoke(singleton("population.skills.SkillLearner"),"bookBudget",b,first);System.out.println("ATTACK BOOK INFO "+name+" bookBudget="+budget+" spendable="+invoke(singleton("population.economy.Errands"),"spendable",b));
   check(((Integer)budget)==b.getAdena(),name+" may spend all its adena on the attack spell book");
   if(after)check(cash-b.getAdena()>0&&cash-b.getAdena()<=130&&b.getSp()<sp,name+" paid only the book and learned for SP");
  }
  System.out.println("ATTACK BOOK INFO learned="+learnedCount+" (purchase needs 115 adena at the Orc Village grocer and 520 SP)");
  System.out.println("POPULATION ATTACK BOOK ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
