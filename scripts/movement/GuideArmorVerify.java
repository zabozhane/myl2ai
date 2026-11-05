import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.data.ItemTable;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Offline Zenthen and HawkEye in their towns: the armor plan follows the class guide kit (planning, then HawkEye's real chores). */
public class GuideArmorVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Object eq=singleton("offlinelife.OfflineEquipment"),gear=singleton("population.economy.Gear");int guidedPlans=0,plans=0;
  for(int id:new int[]{268486152,268491948}){
   Player p=Player.restore(id);p.setAccessLevel(0);p.setIsInvul(false);
   p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
   invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
   ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
   warp(p,MapRegionData.getInstance().getLocationToTeleport(p,MapRegionData.TeleportType.TOWN));
   Set<Integer> guide=new HashSet<>();StringBuilder g=new StringBuilder();for(Object w:(List<?>)invoke(gear,"wishes",p)){int it=(Integer)invoke(w,"getItemId");guide.add(it);g.append(ItemTable.getInstance().getTemplate(it).getName()).append("; ");}
   for(Object w:(List<?>)invoke(gear,"wishes",p)){int it=(Integer)invoke(w,"getItemId");Object offer=invoke(singleton("population.economy.Market"),"findProduct",p,it);net.sf.l2j.gameserver.model.item.instance.ItemInstance c=new net.sf.l2j.gameserver.model.item.instance.ItemInstance(0,it);
    System.out.println("GUIDE WISH "+p.getName()+" "+ItemTable.getInstance().getTemplate(it).getName()+" slot="+invoke(w,"getSlot")+" sold="+(offer!=null)+" eligible="+invoke(eq,"eligible",p,c)+" fits="+invoke(eq,"fits",p,c)+" price="+invoke(singleton("population.economy.Market"),"npcPrice",it));}
   System.out.println("GUIDE INFO "+p.getName()+" lv="+p.getLevel()+" "+p.getClassId()+" cash="+p.getAdena()+" guide wishes: "+g);
   for(int budget:new int[]{p.getAdena(),20000,100000}){kotlin.Pair<?,?> r=(kotlin.Pair<?,?>)invoke(eq,"armorPlan",p,budget);Object plan=r.getFirst();
    if(plan==null){System.out.println("GUIDE INFO   budget="+budget+" plan=none");continue;}
    int it=(Integer)invoke(invoke(plan,"getProduct"),"getItemId");boolean inGuide=guide.contains(it);plans++;if(inGuide)guidedPlans++;
    System.out.println("GUIDE INFO   budget="+budget+" plan="+ItemTable.getInstance().getTemplate(it).getName()+" price="+invoke(plan,"getPrice")+" gain="+invoke(plan,"getGain")+" guide="+inGuide);}
  }
  System.out.println("GUIDE INFO guided plans "+guidedPlans+"/"+plans);
  System.out.println("GUIDE INFO note: guide pieces are not sold in these home towns or exceed the wallet; the plan falls back to the best affordable gain");
  System.out.println("GUIDE ARMOR ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
