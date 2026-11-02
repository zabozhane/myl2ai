import java.util.*;
import net.sf.l2j.gameserver.GameServer;
import dev.l2j.autobots.Autobot;
public class StatusVerify extends MovementVerify {
 public static void main(String[] args){try{
  if(!"yes".equals(System.getenv("L2_MOVEMENT_ISOLATED")))throw new IllegalStateException();
  GameServer.main(new String[0]);Object dao=singleton("dao.AutobotsDao"),service=singleton("movement.MovementStatus");List<Autobot> bots=new ArrayList<>();
  for(String name:new String[]{"Weapons","BALiCTiK","Verzilas","INKOGNITO"}){
   Autobot b=(Autobot)invoke(dao,"loadByName",name);check(b!=null,"snapshot contains silent actor "+name);bots.add(b);
   String text=(String)invoke(service,"line",b);
   check(text.contains("id="+b.getObjectId())&&text.contains(" exp "+b.getExp())&&text.contains("sequence=")&&text.contains("activity=")&&text.contains("hp=")&&text.contains("sitting="),"periodic status covers silent actor "+name);
  }
  invoke(service,"sample",bots);System.out.println("STATUS ALL PASSED");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
