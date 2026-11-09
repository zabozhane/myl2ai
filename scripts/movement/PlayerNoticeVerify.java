import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import net.sf.l2j.gameserver.GameServer;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.ItemTable;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.enums.ZoneId;
import net.sf.l2j.gameserver.enums.actors.ClassId;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.enums.items.CrystalType;
import net.sf.l2j.gameserver.handler.ChatHandler;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.player.Experience;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.model.item.kind.Weapon;
import net.sf.l2j.gameserver.network.GameClient;
import net.sf.l2j.gameserver.network.serverpackets.*;

/**
 * Bots notice a player (docs/player-notice.md), cloud stand only: a plain Player (no bot, no offline farm) with a
 * client that records what it is sent stands among the population and answers like a person would: "да" to a party,
 * the party window accepted, a buff given when asked, haggling and a paid trade for an offer, a whisper after a hello.
 * Run on a throwaway stand with populationAlwaysOnline, populationTestLevel=20 and the LLM off.
 */
public class PlayerNoticeVerify {
 static final List<String[]> heard = new CopyOnWriteArrayList<>();
 static final Map<String, Integer> stats = new ConcurrentHashMap<>();
 static final ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);
 static Player p;

 static void stat(String k) { stats.merge(k, 1, Integer::sum); System.out.println("NOTICE TEST " + k); }
 static Object field(Object o, String name) throws Exception { Field f = o.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(o); }
 static Object singleton(String cls) throws Exception { return Class.forName("dev.l2j.autobots." + cls).getField("INSTANCE").get(null); }
 static Object invoke(Object target, String name, Object... args) throws Exception {
  for (Method m : target.getClass().getMethods()) if (m.getName().equals(name) && m.getParameterCount() == args.length) { m.setAccessible(true); return m.invoke(target, args); }
  throw new NoSuchMethodException(name);
 }

 static void whisper(Player bot, String text) {
  try {
   System.out.println("NOTICE TEST player -> " + bot.getName() + ": " + text);
   Object mgr = singleton("AutobotsManager");
   invoke(mgr, "onChat", 2, p, bot.getName(), text, ChatHandler.getInstance().getHandler(2));
  } catch (Exception e) { e.printStackTrace(); }
 }

 static long price(String text) {
  Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)(kk|k)?").matcher(text);
  long best = 0;
  while (m.find()) { double v = Double.parseDouble(m.group(1)); if ("k".equals(m.group(2))) v *= 1000; if ("kk".equals(m.group(2))) v *= 1_000_000; best = Math.max(best, (long) v); }
  return best;
 }

 static final Map<String, Integer> haggled = new ConcurrentHashMap<>();
 static final Map<String, Long> agreed = new ConcurrentHashMap<>();

 /** What a person does with a line from a bot. */
 static void onLine(int type, String from, String text) {
  Player bot = World.getInstance().getPlayer(from);
  if (bot == null || bot == p) return;
  heard.add(new String[] { String.valueOf(type), from, text });
  System.out.println("NOTICE TEST heard type=" + type + " " + from + ": " + text);
  if (type == 0 && bot.isInsideRadius(p, 1500, false, false)) {
   stat("greeting_heard");
   if (stats.getOrDefault("hello_sent", 0) < 3) { stat("hello_sent"); pool.schedule(() -> whisper(bot, "привет, как дела?"), 3, TimeUnit.SECONDS); }
   return;
  }
  if (type != 2) return;
  String t = text.toLowerCase();
  if (t.contains("бафн") || t.contains("баф ") || t.contains("бб ") || t.startsWith("бб") || t.contains("buff")) {
   stat("buff_asked");
   pool.schedule(() -> { SkillTable.getInstance().getInfo(1040, 1).getEffects(p, bot); stat("buff_given"); }, 4, TimeUnit.SECONDS);
  } else if (t.contains("лавк") || t.contains("shop") || t.contains("store")) {
   stat("store_ad");
  } else if ((t.contains("пати") || t.contains("вместе") || t.contains("к нам")) && t.contains("?")) {
   stat("party_offered");
   pool.schedule(() -> whisper(bot, "да, го"), 3, TimeUnit.SECONDS);
  } else if ((t.contains("надо?") || t.contains("интересно?") || t.contains("не нужен?")) && price(t) > 0) {
   stat("trade_offered");
   agreed.put(from, price(t));
   pool.schedule(() -> whisper(bot, "дорого, скинь"), 3, TimeUnit.SECONDS);
  } else if (t.contains("давай за") || t.contains("и забирай") || t.contains("могу за")) {
   stat("trade_counter");
   agreed.put(from, price(t));
   pool.schedule(() -> whisper(bot, "ок, беру"), 3, TimeUnit.SECONDS);
  } else if (t.contains("кидай трейд")) {
   stat("trade_go");
   pool.schedule(() -> trade(bot, agreed.getOrDefault(from, 0L)), 2, TimeUnit.SECONDS);
  } else stat("other_whisper");
 }

 static void trade(Player bot, long price) {
  try {
   tp(bot.getX() + 30, bot.getY(), bot.getZ());
   Thread.sleep(1500);
   p.onTransactionRequest(bot);
   invoke(singleton("AutobotsManager"), "onTradeRequest", p, bot);
   for (int i = 0; i < 10 && p.getActiveTradeList() == null; i++) Thread.sleep(1000);
   if (p.getActiveTradeList() == null) { stat("trade_not_opened"); return; }
   stat("trade_opened");
   for (int i = 0; i < 15 && p.getActiveTradeList() != null && p.getActiveTradeList().getPartner().getActiveTradeList().getItems().isEmpty(); i++) Thread.sleep(1000);
   long before = p.getAdena();
   p.getActiveTradeList().addItem(p.getInventory().getAdenaInstance().getObjectId(), (int) price);
   Thread.sleep(1000);
   p.getActiveTradeList().confirm();
   for (int i = 0; i < 15 && p.getActiveTradeList() != null; i++) Thread.sleep(1000);
   if (p.getAdena() < before) { stat("trade_done"); System.out.println("NOTICE TEST paid " + (before - p.getAdena()) + " to " + bot.getName()); }
   else stat("trade_failed");
  } catch (Exception e) { e.printStackTrace(); stat("trade_error"); }
 }

 /** A teleport as the client finishes it: the Appearing packet calls onTeleported. */
 static void tp(int x, int y, int z) throws Exception { p.teleportTo(x, y, z, 0); Thread.sleep(500); if (p.isTeleporting()) p.onTeleported(); }

 static GameClient client() {
  return new GameClient(null) {
   @Override public boolean decrypt(java.nio.ByteBuffer b, int s) { return true; }
   @Override public boolean encrypt(java.nio.ByteBuffer b, int s) { return true; }
   @Override public byte[] enableCrypt() { return new byte[0]; }
   @Override public void setState(net.sf.l2j.gameserver.network.GameClient.GameClientState s) {}
   @Override public void close(L2GameServerPacket g) {}
   @Override public synchronized void closeNow() {}
   @Override public synchronized void cleanMe(boolean f) {}
   @Override public boolean isDetached() { return false; }
   @Override public void sendPacket(L2GameServerPacket g) {
    try {
     if (g instanceof CreatureSay) {
      String name = (String) field(g, "_charName"); String text = (String) field(g, "_text"); int type = (Integer) field(g, "_textType");
      if (name != null && text != null) pool.execute(() -> onLine(type, name, text));
     } else if (g instanceof AskJoinParty) {
      String name = (String) field(g, "_requestorName");
      stat("party_window");
      pool.schedule(() -> {
       try { Player r = World.getInstance().getPlayer(name); invoke(singleton("population.group.PartyInvites"), "answer", p, r, true);
        if (p.isInParty()) stat("party_joined"); } catch (Exception e) { e.printStackTrace(); }
      }, 2, TimeUnit.SECONDS);
     }
    } catch (Exception e) { e.printStackTrace(); }
   }
  };
 }

 /** A spare D weapon worth 20k+ on a bot in town, for the trade offer. */
 static void seedTradeGoods() {
  Item d = null;
  for (int id = 1; id < 10000; id++) { Item i = ItemTable.getInstance().getTemplate(id); if (i instanceof Weapon && i.getCrystalType() == CrystalType.D && i.getReferencePrice() > 50000 && i.isTradable()) { d = i; break; } }
  int seeded = 0;
  for (Player b : World.getInstance().getPlayers()) {
   if (b == p || !b.getClass().getSimpleName().equals("Autobot") || !b.isInsideZone(ZoneId.TOWN) || seeded >= 15) continue;
   b.addItem("test", d.getItemId(), 1, null, false); seeded++;
  }
  System.out.println("NOTICE TEST seeded " + seeded + " town bots with " + d.getName());
 }

 public static void main(String[] args) {
  try {
   GameServer.main(new String[0]);
   long minutes = args.length > 0 ? Long.parseLong(args[0]) : 25;
   for (int i = 0; i < 60 && World.getInstance().getPlayers().size() < 40; i++) Thread.sleep(5000);
   System.out.println("NOTICE TEST bots online " + World.getInstance().getPlayers().size());
   Thread.sleep(60000);
   p = Player.create(IdFactory.getInstance().getNextId(), PlayerData.getInstance().getTemplate(ClassId.ELVEN_ORACLE), "noticetest", "Tester" + (System.currentTimeMillis() % 1000), (byte) 0, (byte) 0, (byte) 0, Sex.FEMALE);
   p.setClient(client());
   p.addExpAndSp(Experience.LEVEL[22] - p.getExp(), 0);
   p.addAdena("test", 5_000_000, null, false);
   World.getInstance().addPlayer(p); p.setOnlineStatus(true, true);
   Player first = World.getInstance().getPlayers().stream().filter(b -> b != p).findFirst().get();
   p.spawnMe(first.getX(), first.getY(), first.getZ()); p.onPlayerEnter();
   System.out.println("NOTICE TEST player " + p.getName() + " lvl " + p.getLevel() + " " + p.getClassId() + " live=" + invoke(singleton("population.llm.Attention"), "isLiveAudience", p));
   long end = System.currentTimeMillis() + minutes * 60_000L;
   int hop = 0;
   while (System.currentTimeMillis() < end) {
    if (hop == 2) seedTradeGoods();
    // Stand next to someone new: town bots one hop, bots out hunting the next.
    boolean town = hop % 2 == 0;
    List<Player> bots = new ArrayList<>();
    for (Player b : World.getInstance().getPlayers()) if (b != p && !b.isDead() && b.isInsideZone(ZoneId.TOWN) == town && !b.isInCombat() && (p.getParty() == null || b.getParty() != p.getParty())) bots.add(b);
    if (p.isInParty() && hop % 6 == 5) { p.getParty().removePartyMember(p, null); stat("party_left"); }
    if (!bots.isEmpty() && !p.isInParty()) { Player b = bots.get(new Random().nextInt(bots.size())); tp(b.getX() + 100, b.getY() + 100, b.getZ());
     System.out.println("NOTICE TEST hop " + hop + " to " + b.getName() + " lvl " + b.getLevel() + " town=" + town + " near=" + p.getKnownTypeInRadius(Player.class, 900).size()); }
    hop++;
    Thread.sleep(20_000);
    StringBuilder why = new StringBuilder();
    for (Player b : p.getKnownTypeInRadius(Player.class, 900)) {
     Object seq = null, life = null; try { seq = field(b, "activeSequence"); life = field(b, "life"); } catch (Exception e) {}
     why.append(b.getName()).append("[").append(life == null ? "nolife " : "").append(b.isInCombat() ? "combat " : "").append(b.isInStoreMode() ? "store " : "")
        .append(seq != null ? "seq=" + seq.getClass().getSimpleName() + " " : "").append(b.isInParty() ? "party " : "").append(b.getKarma() > 0 ? "pk " : "").append("] ");
    }
    System.out.println("NOTICE TEST around: " + why);
    Thread.sleep(25_000);
   }
   System.out.println("NOTICE TEST RESULT " + new TreeMap<>(stats));
   boolean ok = stats.containsKey("greeting_heard") && stats.containsKey("party_offered") && stats.containsKey("party_joined");
   System.out.println(ok ? "NOTICE ALL PASSED" : "NOTICE INCOMPLETE");
   System.exit(0);
  } catch (Throwable t) { t.printStackTrace(); System.exit(1); }
 }
}
