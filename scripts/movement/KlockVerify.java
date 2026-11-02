import java.io.*;
import java.lang.reflect.*;
import kotlin.Unit;
import kotlin.coroutines.*;
import kotlin.jvm.functions.Function1;
import kotlinx.coroutines.GlobalScope;

/** A swallowed timer error must expose the exception and call site, not the callback's toString. */
public class KlockVerify extends MovementVerify{
 public static void main(String[] args){try{
  Class<?> type=Class.forName("dev.l2j.autobots.utils.Klock");Constructor<?> ctor=type.getDeclaredConstructor(String.class,long.class,long.class,kotlinx.coroutines.CoroutineScope.class,Function1.class);ctor.setAccessible(true);
  Function1<Continuation<? super Unit>,Object> action=cont->{throw new IllegalStateException("fixture failure");};
  Object timer=ctor.newInstance("fixtureTimer",0L,1000L,GlobalScope.INSTANCE,action);
  @SuppressWarnings("unchecked") Function1<Continuation<? super Unit>,Object> run=(Function1<Continuation<? super Unit>,Object>)member(timer,"tryAction").get(timer);
  ByteArrayOutputStream out=new ByteArrayOutputStream(),err=new ByteArrayOutputStream();PrintStream oldOut=System.out,oldErr=System.err;
  try{System.setOut(new PrintStream(out));System.setErr(new PrintStream(err));
   run.invoke(new Continuation<Unit>(){public CoroutineContext getContext(){return EmptyCoroutineContext.INSTANCE;}public void resumeWith(Object result){kotlin.ResultKt.throwOnFailure(result);}});
  }finally{System.setOut(oldOut);System.setErr(oldErr);}
  check(out.toString("UTF-8").contains("fixtureTimer timer action failed: java.lang.IllegalStateException: fixture failure"),"timer reports exception type and message");
  check(err.toString("UTF-8").contains("IllegalStateException: fixture failure")&&err.toString("UTF-8").contains("KlockVerify"),"timer reports stack with failing call site");
  System.out.println("KLOCK DIAGNOSTICS ALL PASSED");
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
