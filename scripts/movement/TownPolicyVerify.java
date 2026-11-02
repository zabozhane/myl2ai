public class TownPolicyVerify extends MovementVerify {
    public static void main(String[] args) {
        try {
            Object p=singleton("movement.TownPolicy");
            check(invoke(p,"reason",false,false,true,false,false,false,false,false,3600000L,180000L)==null,"three productive minutes do not trigger a quest SoE");
            check("quest_ready".equals(invoke(p,"reason",false,false,true,false,false,false,false,false,3600000L,660000L)),"quest turn-in becomes available after useful hunting");
            check(invoke(p,"reason",false,false,true,false,false,false,false,false,180000L,660000L)==null,"optional errands respect the interval between city visits");
            check("healing_stock".equals(invoke(p,"reason",false,true,true,false,false,false,false,false,0L,0L)),"urgent healing is not blocked by the optional timer");
            check("weight".equals(invoke(p,"reason",true,false,true,false,false,false,false,false,0L,0L)),"overload is handled immediately");
            check("training".equals(invoke(p,"reason",false,false,false,false,true,false,false,false,3600000L,0L)),"required training can prepare an unproductive novice");
            check(invoke(p,"reason",false,false,false,true,false,true,true,true,3600000L,0L)==null,"commute time alone does not trigger routine purchases");
            check("routine_sale".equals(invoke(p,"reason",false,false,false,true,false,false,false,false,7200000L,660000L)),"routine sales remain possible");
            System.out.println("TOWN POLICY ALL PASSED");
        }catch(Throwable error){error.printStackTrace();System.exit(1);}
    }
}
