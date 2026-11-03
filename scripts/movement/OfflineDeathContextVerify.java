import java.util.*;
import java.util.logging.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.instance.Monster;
import net.sf.l2j.gameserver.model.actor.player.Experience;
import net.sf.l2j.gameserver.model.location.Location;
import net.sf.l2j.gameserver.enums.IntentionType;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.geoengine.GeoEngine;
import dev.l2j.autobots.autofarm.AutofarmManager;

/** Controlled owner level/inventory; an ordinary NPC causes real death, core penalties and one diagnostic event. */
public class OfflineDeathContextVerify extends WeaponGradeSuppliesVerify {
 @SuppressWarnings("unchecked") public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();GameServer.main(new String[0]);Object manager=singleton("offlinelife.OfflineFarmManager");invoke(member(manager,"travelTask").get(manager),"cancel");invoke(member(AutofarmManager.INSTANCE,"onUpdateTask").get(AutofarmManager.INSTANCE),"cancel");
  Player p=Player.restore(268486152);check(p!=null,"owner death diagnostic fixture restores");p.getStat().setLevel((byte)13);p.getStat().setExp(Experience.LEVEL[13]+1);p.setAccessLevel(0);p.setIsInvul(false);p.setClient((net.sf.l2j.gameserver.network.GameClient)Class.forName("dev.l2j.autobots.offlinelife.DetachedClient").getConstructor(Player.class).newInstance(p));invoke(AutofarmManager.INSTANCE,"onEnterWorld",p);World.getInstance().addPlayer(p);p.setOnlineStatus(true,true);p.spawnMe(p.getX(),p.getY(),p.getZ());p.onPlayerEnter();Map<Integer,Player> farmers=(Map<Integer,Player>)member(manager,"farming").get(manager);farmers.put(p.getObjectId(),p);invoke(manager,"openJournal",p);invoke(AutofarmManager.INSTANCE,"quiesce",p);
  for(int id:new int[]{1060,1061,1539,1540})erase(p,id);p.getInventory().addItem("Death report controlled stock",1060,7,p,null);
  List<String> messages=Collections.synchronizedList(new ArrayList<>());Logger logger=Logger.getLogger(manager.getClass().getName());Handler capture=new Handler(){public void publish(LogRecord r){if(r.getMessage().startsWith("Offline farm death: "))messages.add(r.getMessage());}public void flush(){}public void close(){}};logger.addHandler(capture);
  List<Monster> rats=new ArrayList<>();for(Object o:World.getInstance().getObjects())if(o instanceof Monster){Monster m=(Monster)o;if(m.getNpcId()==20359&&!m.isDead()&&!m.isInsideZone(ZoneId.TOWN))rats.add(m);}rats.sort(Comparator.comparingDouble(m->Math.hypot(m.getX()+28856,m.getY()-133144)));Monster enemy=null;GeoEngine geo=GeoEngine.getInstance();
  for(Monster m:rats){int x=m.getX()-80,y=m.getY(),z=geo.getHeight(x,y,m.getZ());if(geo.canMoveToTarget(x,y,z,m.getX(),m.getY(),m.getZ())){enemy=m;warp(p,new Location(x,y,z));break;}}check(enemy!=null&&!p.isInsideZone(ZoneId.PEACE),"actual field NPC can attack under normal rules");p.getAI().setIntention(IntentionType.IDLE);p.setTarget(null);long xp=p.getExp();int level=p.getLevel();enemy.addDamageHate(p,0,100);enemy.getAI().setIntention(IntentionType.ATTACK,p);long until=System.currentTimeMillis()+60000;while(!p.isDead()&&System.currentTimeMillis()<until)Thread.sleep(100);check(p.isDead(),"ordinary NPC combat really causes owner death without injected lethal damage");Thread.sleep(250);
  check(messages.size()==1,"successful core death emits exactly one offline diagnostic event");String msg=messages.get(0);check(msg.contains("Zenthen id="+p.getObjectId()+" lv "+level+" ")&&msg.contains("Ratman Warrior (20359) lv "),"diagnostic identifies the actual victim, pre-penalty level and killer");check(msg.contains("healing=7 ")&&msg.contains("expBeforePenalty="+xp)&&msg.contains("weapon=")&&msg.contains("route=")&&msg.contains("at="),"diagnostic retains exact owned healing stock, pre-penalty XP, weapon and activity location");check(p.getLevel()<level&&p.getExp()<xp,"ordinary death still applies the real level/experience penalty after reporting");check(!p.doDie(enemy)&&messages.size()==1,"repeated doDie on the same corpse neither reports twice nor applies a second death");check(!p.isGM()&&!p.isInvul(),"death capture needs no GM, invulnerability or HP replacement");
  p.doRevive();check(!p.isDead(),"ordinary revival remains available");farmers.remove(p.getObjectId());p.doDie(null);check(messages.size()==1,"owner handoff prevents further offline death reporting");logger.removeHandler(capture);System.out.println("OFFLINE DEATH CONTEXT INFO level="+level+"->"+p.getLevel()+" xp="+xp+"->"+p.getExp());System.out.println("OFFLINE DEATH CONTEXT ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
