/** Reuse real selection/arrival/combat with controlled failures of nearby paths. */
public class DistantFieldRecoveryVerify {
 public static void main(String[] args){String[] flags=new String[args.length+1];flags[0]="--exclude-local";System.arraycopy(args,0,flags,1,args.length);NoviceTargetsVerify.main(flags);}
}
