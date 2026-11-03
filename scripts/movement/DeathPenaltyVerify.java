import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.Autobot;
import dev.l2j.autobots.autofarm.AutofarmManager;

/**
 * Controlled Death Penalty levels (core setter, which applies the real debuff); no scroll, money or GM is given.
 * Malibu (resident lv 14, own adena): buys No Grade scrolls at a real grocer, reads them with the reuse delay.
 * Zenthen (offline lv 20, owns one No Grade scroll): the wrong version stays unused, the D scroll waits for money.
 */
public class DeathPenaltyVerify extends OfflineTownVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object m=singleton("offlinelife.OfflineFarmManager");invoke(member(m,"travelTask").get(m),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Object dp=singleton("population.economy.DeathPenaltyRecovery");Object market=singleton("population.economy.Market");
  // Resident: real purchase and use.
  Autobot b=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","ShRaM");check(b!=null,"resident ShRaM restores");
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",b);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(b.getName());invoke(b,"cancelSequence");Thread.sleep(300);b.setAccessLevel(0);b.setIsInvul(false);
  Location home=net.sf.l2j.gameserver.data.xml.MapRegionData.getInstance().getLocationToTeleport(b,net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType.TOWN);warp(b,home);int scroll=(Integer)invoke(dp,"scrollFor",b);check(scroll==8594,"a level 14 character reads the No Grade version");
  kotlin.Pair<?,?> offer=(kotlin.Pair<?,?>)invoke(market,"findProduct",b,scroll);check(offer!=null,"an ordinary grocer of the home town sells it");
  Npc seller=(Npc)invoke(offer.getFirst(),"getMerchant");warp(b,new Location(seller.getX()+40,seller.getY()+40,seller.getZ()));
  int price=(Integer)invoke(market,"priceWithTax",offer.getFirst(),offer.getSecond());
  b.setDeathPenaltyBuffLevel(0);int cash0=b.getAdena();
  kotlin.jvm.functions.Function2<Object,Object,Object> here=(npc,cont)->Boolean.TRUE;
  suspendValue(dp,"lift",b,cash0,here);check(b.getAdena()==cash0&&b.getInventory().getInventoryItemCount(8594,-1)==0,"zero penalty spends nothing");
  b.setDeathPenaltyBuffLevel(3);int cash=b.getAdena(),budget=(Integer)invoke(singleton("population.economy.Errands"),"spendable",b);
  System.out.println("DP INFO resident cash="+cash+" budget="+budget+" price="+price+" penalty=3");
  long began=System.currentTimeMillis();suspendValue(dp,"lift",b,budget,here);
  int bought=(cash-b.getAdena())/price;System.out.println("DP INFO resident after penalty="+b.getDeathPenaltyBuffLevel()+" cash="+cash+"->"+b.getAdena()+" bought="+bought+" left="+b.getInventory().getInventoryItemCount(8594,-1)+" seconds="+(System.currentTimeMillis()-began)/1000);
  check(bought>=1&&cash-b.getAdena()==bought*price,"real purchase at the grocer's taxed price");
  check(b.getDeathPenaltyBuffLevel()==3-bought&&b.getInventory().getInventoryItemCount(8594,-1)==0,"each bought scroll is read and removes one penalty level");
  check(bought==3&&b.getDeathPenaltyBuffLevel()==0,"three levels are lifted one after another with the reuse delay");
  check(!(Boolean)invoke(dp,"worthTrip",b,b.getAdena()),"no penalty is no reason for a trip");b.setDeathPenaltyBuffLevel(1);check((Boolean)invoke(dp,"worthTrip",b,b.getAdena()),"a payable penalty is a reason to visit town");b.setDeathPenaltyBuffLevel(0);
  // A modest wallet below the generic reserve still pays: the penalty is essential like healing.
  Autobot poor=(Autobot)invoke(singleton("dao.AutobotsDao"),"loadByName","Sladenkaya");check(poor!=null,"Sladenkaya restores");
  suspendValue(singleton("AutobotsManager"),"spawnAutobot",poor);((Map<?,?>)invoke(singleton("AutobotsManager"),"getActiveBots")).remove(poor.getName());invoke(poor,"cancelSequence");Thread.sleep(300);poor.setAccessLevel(0);poor.setIsInvul(false);
  warp(poor,net.sf.l2j.gameserver.data.xml.MapRegionData.getInstance().getLocationToTeleport(poor,net.sf.l2j.gameserver.data.xml.MapRegionData.TeleportType.TOWN));
  kotlin.Pair<?,?> poorOffer=(kotlin.Pair<?,?>)invoke(market,"findProduct",poor,8594);check(poorOffer!=null,"Sladenkaya's town sells it");Npc poorSeller=(Npc)invoke(poorOffer.getFirst(),"getMerchant");warp(poor,new Location(poorSeller.getX()+40,poorSeller.getY()+40,poorSeller.getZ()));
  int poorPrice=(Integer)invoke(market,"priceWithTax",poorOffer.getFirst(),poorOffer.getSecond());poor.setDeathPenaltyBuffLevel(1);int poorCash=poor.getAdena();
  System.out.println("DP INFO small wallet cash="+poorCash+" spendable="+invoke(singleton("population.economy.Errands"),"spendable",poor)+" price="+poorPrice);
  suspendValue(dp,"lift",poor,poorCash,here);System.out.println("DP INFO small wallet penalty="+poor.getDeathPenaltyBuffLevel()+" cash="+poorCash+"->"+poor.getAdena());
  check(poor.getDeathPenaltyBuffLevel()==0&&poorCash-poor.getAdena()==poorPrice,"below the generic reserve the scroll is still bought and read");
  // Offline owner: wrong owned version, D scroll needs money.
  Player p=Player.restore(268486152);check(p!=null,"Zenthen restores");p.setAccessLevel(0);p.setIsInvul(false);
  p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));
  invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();
  ((Map<Integer,Player>)member(m,"farming").get(m)).put(p.getObjectId(),p);invoke(m,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  if(!p.isInsideZone(ZoneId.TOWN))warp(p,new Location(12056,16568,GeoEngine.getInstance().getHeight(12056,16568,-4584)));
  p.setDeathPenaltyBuffLevel(1);long ng=p.getInventory().getInventoryItemCount(8594,-1);check((Integer)invoke(dp,"scrollFor",p)==8595,"level 20 needs the Grade D version");
  suspendValue(singleton("offlinelife.OfflineChores"),"run",p);
  System.out.println("DP INFO offline penalty="+p.getDeathPenaltyBuffLevel()+" ownedNoGrade="+ng+"->"+p.getInventory().getInventoryItemCount(8594,-1)+" cash="+p.getAdena());
  check(p.getDeathPenaltyBuffLevel()==1&&p.getInventory().getInventoryItemCount(8594,-1)==ng,"the wrong version is not wasted and a small wallet waits");
  check(!b.isGM()&&!p.isGM(),"no GM");
  System.out.println("DEATH PENALTY ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
