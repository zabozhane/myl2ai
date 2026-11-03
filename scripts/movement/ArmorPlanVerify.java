import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.items.ArmorType;
import dev.l2j.autobots.Autobot;

/** Planning only (no purchase, no money given): armor plans for a resident fighter and a resident mage in their home towns. */
public class ArmorPlanVerify extends OfflineTownVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object eq=singleton("offlinelife.OfflineEquipment");Object dao=singleton("dao.AutobotsDao");
  for(String name:new String[]{"Malibu","DarieL","Jusis","Govard"}){
   Autobot b=(Autobot)invoke(dao,"loadByName",name);if(b==null){System.out.println("ARMOR PLAN INFO missing "+name);continue;}
   Location town=MapRegionData.getInstance().getLocationToTeleport(b,MapRegionData.TeleportType.TOWN);b.setXYZInvisible(town.getX(),town.getY(),town.getZ());
   for(int budget:new int[]{b.getAdena(),3000}){
    kotlin.Pair<?,?> r=(kotlin.Pair<?,?>)invoke(eq,"armorPlan",b,budget);Object plan=r.getFirst(),goal=r.getSecond();
    String d=plan==null?"none":invoke(invoke(invoke(plan,"getProduct"),"getItem"),"getName")+" slot="+invoke(plan,"getSlot")+" gain="+invoke(plan,"getGain")+" price="+invoke(plan,"getPrice");
    System.out.println("ARMOR PLAN INFO "+name+" lv="+b.getLevel()+" class="+b.getClassId()+" mage="+b.isMageClass()+" cash="+b.getAdena()+" budget="+budget+" plan="+d+" goal="+(goal==null?"none":invoke(invoke(invoke(goal,"getProduct"),"getItem"),"getName")+"/"+invoke(goal,"getPrice")));
    if(plan!=null){net.sf.l2j.gameserver.model.item.kind.Item it=(net.sf.l2j.gameserver.model.item.kind.Item)invoke(invoke(plan,"getProduct"),"getItem");
     check((Double)invoke(plan,"getGain")>0&&(Integer)invoke(plan,"getPrice")<=budget,name+" plan adds defence within budget");
     check(!(b.isMageClass()&&it.getItemType()==ArmorType.HEAVY),name+" mage is not planned into heavy armor");
     check(it.getCrystalType().getId()<=dev.l2j.autobots.population.economy.Gear.INSTANCE.gradeFor(b.getLevel()).getId(),name+" grade is legal for the level");}
   }}
  System.out.println("ARMOR PLAN ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
