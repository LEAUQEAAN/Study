import com.sun.org.apache.bcel.internal.generic.FADD;
import com.sun.org.apache.regexp.internal.RE;
import com.sun.org.apache.xml.internal.resolver.readers.TR9401CatalogReader;
import com.sun.xml.internal.bind.v2.model.core.ID;
import com.sun.xml.internal.stream.util.BufferAllocator;
import com.sun.xml.internal.ws.encoding.soap.SOAP12Constants;
import javafx.geometry.Pos;

import java.io.*;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

/**********************************************************
 *  时间：2018-11-25                                      *
 *  学号：51184501130                                     *
 *  姓名：刘骞                                            *
 *  说明：                                                *
 *      如果需要替换测试用例，则                           *
 *          1.需将输入文件的路径进行修改                   *
 *          2.需将输出文件的路径进行修改                   *
 *          3.然后再把程序执行一边即可                     *
 *                                                       *
 *********************************************************/
@SuppressWarnings("all")
public class Main {


    //存放Pre_Issue的指令
    static List<Introduction> Pre_Issue_Buffer_list = new ArrayList<>();

    //存放Pre_ALU_Queue的指令
    static List<Introduction> Pre_ALU_Queue_list = new ArrayList<>();

    static Introduction Post_ALU_Buffer = null;

    static List<Introduction> Pre_ALUB_Queue_list = new ArrayList<>();

    static Introduction Post_ALUB_Buffer = null;

    static List<Introduction> Pre_MEM_Queue_list = new ArrayList<>();

    static Introduction Post_MEM_Buffer = null;

    static Introduction IF_Unit_Wait = null;

    static Introduction IF_Unit_Excute = null;

    static int canFetch = 0;

    static boolean CAN_ALU_EXCUTE;
    static boolean CAN_ALUB_EXCUTE;
    static boolean CAN_MEM_EXCUTE;
    static boolean CAN_BRANCH_EXCUTE;


    static int COUNT_PRE;
    static int COUNT_ALU;
    static int COUNT_ALUB;
    static int COUNT_MEM;
    static int op_cnt = 0;


    static String BranchInstruction = "J,JR,BEQ,BLTZ,BGTZ,BREAK,NOP";

    static String ALUInstruction = "ADD,ADDI,SUB,SUBI,NOR,NORI,AND,ANDI,SLT,SLTI";

    static String ALUBInstruction = "SLL,SRL,SRA,MUL,MULI";

    static String MEMInstruction = "LW,SW";

    /**
     * 保留区 0 CON 1 MEM 2 ALU 3 ALUB
     */
    static Reserve[] reserves = {new Reserve("CON"),new Reserve("MEM"),new Reserve("ALU"),new Reserve("ALUB")};

    /**
     * 保留区对象
     */
    static class Reserve{
        public String name;     //名称
        public String estr = "";     //名称
        public boolean busy = false;    //是否被占用
        public String op = "";      //操作码

        public int Vi = 0;       //操作数1
        public int Vj = 0;       //操作数1
        public int Vk = 0;       //操作数1

        public String Qj = "";       //操作数1-对应
        public String Qk = "";       //操作数2-对应
        public boolean Rj = false;      //操作数1是否准备好了
        public boolean Rk = false;      //操作数2是否准备好了

        public boolean canChangeState = true ;
        public int A ;        //地址

        public Reserve(String name) {
            this.name = name;
        }
        public Reserve() {
        }
    }

    //二进制的输入文件路径
    static String InputFilePath = "sample.txt";

    //反汇编的结果文件路径
    static String OuputFilePath1 = "disassembly.txt";

    //执行结果文件路径
    static String OuputFilePath2 = "simulation.txt";

    //标志是否编译完了
    static Boolean finished = false ;

    //是否没执行完
    static Boolean finish = true;

    //程序计数器，存放的是当前执行的命令的下标
    static int PC = 0 ;

    //寄存器
    static int[] R = new int[32];
    static String[] RegState = {"","","","","","","","","","","","","","","","","","","","","","","","","","","","","","","",""};

    //存放地址数据
    static List<Data> datas = new ArrayList<>();

    /**
     * 内存数据对象
     */
    static class Data{
        public int address; //地址
        public int data;    //数据
    }

    //存放指令集合
    static List<Introduction> its = new ArrayList<>();

    /**
     * 汇编指令对象
     */
    static class Introduction{
        public String name;     //操作名称
        public int rs ;         //从左往右 【6-11】
        public int rt ;         //从左往右 【11-16】
        public int rd ;         //从左往右 【16-21】
        public int sa ;         //从左往右 【21-26】
        public int address;     //指令地址
        public int immediate ;  //立即数
        public String estr ;    //反编译的指令
        public String detail;   //二进制码
        public boolean operate = false;
        public int run_time = 0;

    }

    public static void main(String[] args) throws Exception {

        if(args.length == 2){
            InputFilePath = args[0];
            OuputFilePath2 = args[1];
        }

        /************************反汇编并写入文件中******************************/

        BufferedWriter bw1 =new BufferedWriter(new FileWriter(new File(OuputFilePath1)));

        List<String> ls = loadFiles();
        int count = 0;

        for (String itr : ls ) {
            disassembly(itr, count, bw1);
            count++;
        }
        bw1.close();

        /*************************执行汇编指令*******************************/

        count = 0 ;
        BufferedWriter bw2 =new BufferedWriter(new FileWriter(new File(OuputFilePath2)));
        PC = 0 ;
        while (finish) {
            if(count == 2){
                System.out.println();
            }
            op_cnt = 0 ;
            System.out.println("--------------------\nCycle:" + (count+1));// + "\t" + idct.address + "\t" + idct.estr);

            bw2.write("--------------------\nCycle:" + (count+1) );//+ "\t" + idct.address + "\t" + idct.estr);


            check();

            if (IF_Unit_Wait!=null && CON_BeIso(IF_Unit_Wait)  &&  IF_Unit_Wait.operate==false) {
                prepare_operands(IF_Unit_Wait);
                IF_Unit_Wait.operate = true;
            }


            if(CAN_BRANCH_EXCUTE && IF_Unit_Excute !=null){
                excute("CON");
                IF_Unit_Excute= null;
                canFetch = 0;
            }


            for (Reserve r : reserves){
                r.canChangeState = true;
            }

            //取指
            if(canFetch==0) {
                instruct_fetch();
            }




            //发射指令
            instruct_issue();



            //执行指令
            excute();

            if(Post_ALU_Buffer!=null){
                Introduction it = Pre_ALUB_Queue_list.get(0);
                it.run_time = 1;
                Pre_ALUB_Queue_list.set(0,it);
            }

            //数据准备完毕
            post_buffer();

            printBuffer(bw2);
            printRegs(bw2);
            printDatas(bw2);
            count++;

            if(PC == -1) break;


        }

        bw2.close();

    }



    private static void check() {

        COUNT_PRE= Pre_Issue_Buffer_list.size();
        COUNT_ALU = Pre_ALU_Queue_list.size();
        COUNT_ALUB = Pre_ALUB_Queue_list.size();
        COUNT_MEM = Pre_MEM_Queue_list.size();


        CAN_ALU_EXCUTE =  Post_ALU_Buffer != null ;// true;// Post_ALU_Buffer != null;
        CAN_ALUB_EXCUTE= Post_ALUB_Buffer != null ;//  Post_ALUB_Buffer != null;
        CAN_MEM_EXCUTE =  Post_MEM_Buffer != null ;// Post_MEM_Buffer!= null;
        CAN_BRANCH_EXCUTE = IF_Unit_Excute != null ;
    }


    private static void excute() {
        if(CAN_ALU_EXCUTE &&  Post_ALU_Buffer!=null){
            excute("ALU");
            Post_ALU_Buffer = null;
        }
        if(CAN_ALUB_EXCUTE && Post_ALUB_Buffer!=null){
            excute("ALUB");
            Post_ALUB_Buffer = null;
        }
        if(CAN_MEM_EXCUTE&& Post_MEM_Buffer!=null ){
            excute("MEM");
            Post_MEM_Buffer= null;
        }

    }

    static void resetReserve(int i){
        reserves[i].op = "";
        reserves[i].Vi = 0;
        reserves[i].Vj = 0;
        reserves[i].Vk = 0;
        reserves[i].Qj = "";
        reserves[i].Qk = "";
        reserves[i].Rj = false;
        reserves[i].Rk = false;
    }

    /**
     * 广播
     * @param idx
     * @param ridx
     */
    static void scr(int idx,int ridx){

        for(Reserve r : reserves){
            if(r.Qj.equals(reserves[idx].name)){
                r.Vj = R[ridx];
                r.Qj = "";
                r.Rj = true;
                r.canChangeState = false;
            }
            if(r.Qk.equals(reserves[idx].name)){
                r.Vk = R[ridx];
                r.Qk = "";
                r.Rk = true;
                r.canChangeState = false;
            }
        }
        RegState[ridx] = "";
    }

    private  static  int getRidx(String name){
        for (int i = 0 ; i < 32 ; i++){
            if(RegState[i].equals(name)) return i;
        }
        return -1 ;
    }

    /**
     * 通过地址获取指令下标
     * @param address
     * @return
     */
    private static int getIndexByAddress(int address){
        for (int i = 0 ; i < its.size() ;i++){
            if(address==its.get(i).address) return i ;
        }
        return -1;
    }

    //执行
    private static void excute(String NAME) {
        int ridx = 0;
        switch (NAME){
            case "CON":
                if(reserves[0].op.equals("J")){
                    System.out.println(reserves[0].Vi);
                    System.out.println(getIndexByAddress(reserves[0].Vi));
                    PC = getIndexByAddress(reserves[0].Vi) ;
                }else if(reserves[0].op.equals("JR")){
                    PC = getIndexByAddress(reserves[0].Vj);
                }else if(reserves[0].op.equals("BEQ")){
                    if(reserves[0].Vj == reserves[0].Vk ){
                        PC = PC + reserves[0].Vi/4;
                    }
                }else if(reserves[0].op.equals("BGTZ")){
                    if(reserves[0].Vj > 0){
                        PC = PC + reserves[0].Vi/4;
                    }
                }else if(reserves[0].op.equals("BLTZ")){
                    if(reserves[0].Vj < 0){
                        PC = PC + reserves[0].Vi/4;
                    }
                }

                break;
            case "MEM":
                if(reserves[1].op.equals("LW")){
                    ridx = getRidx("MEM");
                    for(int i=0;i< datas.size() ;i++ ){
                        Data data = datas.get(i);
                        if (data.address == (reserves[1].Vj+ reserves[1].Vk)) {
                            R[ridx] = data.data;
                            break;
                        }
                    }
                    scr(1,ridx);
                }else if(reserves[1].op.equals("SW")){
                    for(int i=0;i< datas.size() ;i++ ){
                        Data data = datas.get(i);
                        if (data.address == (reserves[1].Vj+ reserves[1].Vk)) {
                            data.data = R[reserves[1].Vi];
                            datas.set(i,data);
                            break;
                        }
                    }
                }
                resetReserve(1);
                break;
            case "ALU":
                ridx = getRidx("ALU");
                if(reserves[2].op.equals("ADD") ||reserves[2].op.equals("ADDI")){
                    System.out.println(reserves[2].Vj +"--"+ reserves[2].Vk);
                    R[ridx] = reserves[2].Vj + reserves[2].Vk;

                }else if(reserves[2].op.equals("SUB") ||reserves[2].op.equals("SUBI")){
                    R[ridx] = reserves[2].Vj - reserves[2].Vk;
                }else if(reserves[2].op.equals("AND") ||reserves[2].op.equals("ANDI")){
                    R[ridx] = reserves[2].Vj & reserves[2].Vk;
                }else if(reserves[2].op.equals("NOR") ||reserves[2].op.equals("NORI")){
                    R[ridx] = reserves[2].Vj ^ reserves[2].Vk;
                }else if(reserves[2].op.equals("SLT") ||reserves[2].op.equals("SLTI")){
                    R[ridx] = reserves[2].Vj < reserves[2].Vk ? 1 : 0 ;

                }
                scr(2,ridx);
                resetReserve(2);
                break;
            case "ALUB":
                ridx = getRidx("ALUB");
                if(reserves[3].op.equals("MUL") ||reserves[3].op.equals("MULI")){
                    R[ridx] = reserves[3].Vj * reserves[3].Vk;
                }else if(reserves[3].op.equals("SRL")){
                    R[ridx] = reserves[3].Vj >>> reserves[3].Vk;
                }else if(reserves[3].op.equals("SRA")){
                    R[ridx] = reserves[3].Vj >> reserves[3].Vk;
                }else if(reserves[3].op.equals("SLL")){
                    System.out.println(reserves[3].Vj +"---"+ reserves[3].Vk);
                    System.out.println((int)(reserves[3].Vj * Math.pow(2,reserves[3].Vk)));
                    System.out.println(ridx);
                    R[ridx] = (int)((reserves[3].Vj * Math.pow(2,reserves[3].Vk)));
                    System.out.println(R[ridx]);
                }
                System.out.println(ridx);
                scr(3,ridx);
                resetReserve(3);


        }



    }


    /**
     * 指令发射
     */
    private static void instruct_issue() {
        int n = 0 ;
        for (int i = 0 ; i < COUNT_PRE && i < Pre_Issue_Buffer_list.size() ; i++) {
            Introduction it = Pre_Issue_Buffer_list.get(i);
            if(NoWAW(it) /*&& NoWAR(it)*/ && BeIso(i) ){
                n++;
                if(isALU(it) && Pre_ALU_Queue_list.size() < 2){
                    Pre_ALU_Queue_list.add(it);
                    Pre_Issue_Buffer_list.remove(i);
                    i--;
                }else if(isALUB(it)&& Pre_ALUB_Queue_list.size() < 2 ){
                    it.run_time = 0;
                    Pre_ALUB_Queue_list.add(it);
                    Pre_Issue_Buffer_list.remove(i);
                    i--;
                }else if(isMEM(it) && Pre_MEM_Queue_list.size() < 2 ){
                    Pre_MEM_Queue_list.add(it);
                    Pre_Issue_Buffer_list.remove(i);
                    i--;
                }/*else if(isBranch(it) && IF_Unit_Wait == null){
                    IF_Unit_Wait = it;
                    Pre_Issue_Buffer_list.remove(i);
                    i--;
                    break;
                }*/
                else
                    n -- ;
            }
        }

        /*for(int i = 0 ; i < n ; i++){
            Pre_Issue_Buffer_list.remove(0);
        }*/
    }


    private static void saveWs(Introduction idct,List<Integer> ws){
        switch (idct.name){
            case "J":
                break;
            case "JR":
                ws.add(idct.rs);
                break;
            case "ADD":
                ws.add(idct.rd);
                break;
            case "ADDI":
                ws.add(idct.rt);
                break;
            case "NOR":
                ws.add(idct.rd);
                break;
            case "NORI":
                ws.add(idct.rt);
                break;
            case "SLT":
                ws.add(idct.rd);
                break;
            case "SLTI":
                ws.add(idct.rt);
                break;
            case "SUB":
                ws.add(idct.rd);
                break;
            case "SUBI":
                ws.add(idct.rt);
                break;
            case "SLL":
                ws.add(idct.rd);
                break;
            case "SRL":
                ws.add(idct.rd);
                break;
            case "SRA":
                ws.add(idct.rd);
                break;
            case "MUL":
                ws.add(idct.rd);
                break;
            case "MULI":
                ws.add(idct.rt);
                break;
            case "LW":
                ws.add(idct.rt);
                break;
            case "SW":
                break;
            case "BGTZ":
                break;
            case "BLTZ":
                break;
            case "BEQ":
                break;
            case "AND":
                ws.add(idct.rd);
                break;
            case "ANDI":
                ws.add(idct.rt);
                break;

        }
    }

    //检测该指令是否可以提前执行
    private static boolean BeIso(int idx) {
        List<Integer> ws = new ArrayList<>();
        List<Integer> rs = new ArrayList<>();



        for(int i=0;i< idx;i++){
            Introduction idct = Pre_Issue_Buffer_list.get(i);
            saveWs(idct,ws);
        }
        for(int i=0;i< Pre_ALU_Queue_list.size();i++){
            Introduction idct = Pre_ALU_Queue_list.get(i);
            saveWs(idct,ws);
        }
        for(int i=0;i< Pre_ALUB_Queue_list.size();i++){
            Introduction idct = Pre_ALUB_Queue_list.get(i);
            saveWs(idct,ws);
        }
        for(int i=0;i< Pre_MEM_Queue_list.size();i++){
            Introduction idct = Pre_MEM_Queue_list.get(i);
            saveWs(idct,ws);
        }

        if(Post_ALU_Buffer!=null) saveWs(Post_ALU_Buffer,ws);
        if(Post_ALUB_Buffer!=null) saveWs(Post_ALUB_Buffer,ws);
        if(Post_MEM_Buffer!=null) saveWs(Post_MEM_Buffer,ws);



        Introduction idct = Pre_Issue_Buffer_list.get(idx);

        switch (idct.name){
            case "J":
                return true;
            case "JR":
                return !ws.contains(idct.rs);
            case "ADD":
                return !ws.contains(idct.rd) && !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "ADDI":
                return !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "NOR":
                return !ws.contains(idct.rd) && !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "NORI":
                return !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "SLT":
                return !ws.contains(idct.rd) && !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "SLTI":
                return !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "SUB":
                return !ws.contains(idct.rd) && !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "SUBI":
                return !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "SLL":
                return !ws.contains(idct.rt) && !ws.contains(idct.rd);
            case "SRL":
                return !ws.contains(idct.rt) && !ws.contains(idct.rd);
            case "SRA":
                return !ws.contains(idct.rt) && !ws.contains(idct.rd);
            case "MUL":
                return !ws.contains(idct.rd) && !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "MULI":
                return !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "LW":
                return !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "SW":
                return !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "BGTZ":
                return  !ws.contains(idct.rs);
            case "BLTZ":
                return  !ws.contains(idct.rs);
            case "BEQ":
                return !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "AND":
                return !ws.contains(idct.rd) && !ws.contains(idct.rt) && !ws.contains(idct.rs);
            case "ANDI":
                return !ws.contains(idct.rt) && !ws.contains(idct.rs);
            default:
                return false;
        }

    }

    //检测该指令是否可以提前执行
    private static boolean CON_BeIso(Introduction id) {
        List<Integer> ws = new ArrayList<>();
        List<Integer> rs = new ArrayList<>();



        for(int i=0;i< Pre_Issue_Buffer_list.size();i++){
            Introduction idct = Pre_Issue_Buffer_list.get(i);
            saveWs(idct,ws);
        }
        for(int i=0;i< Pre_ALU_Queue_list.size();i++){
            Introduction idct = Pre_ALU_Queue_list.get(i);
            saveWs(idct,ws);
        }
        for(int i=0;i< Pre_ALUB_Queue_list.size();i++){
            Introduction idct = Pre_ALUB_Queue_list.get(i);
            saveWs(idct,ws);
        }
        for(int i=0;i< Pre_MEM_Queue_list.size();i++){
            Introduction idct = Pre_MEM_Queue_list.get(i);
            saveWs(idct,ws);
        }

        if(Post_ALU_Buffer!=null) saveWs(Post_ALU_Buffer,ws);
        if(Post_ALUB_Buffer!=null) saveWs(Post_ALUB_Buffer,ws);
        if(Post_MEM_Buffer!=null) saveWs(Post_MEM_Buffer,ws);



        switch (id.name){
            case "J":
                return true;
            case "JR":
                return !ws.contains(id.rs);
            case "BGTZ":
                return  !ws.contains(id.rs);
            case "BLTZ":
                return  !ws.contains(id.rs);
            case "BEQ":
                return !ws.contains(id.rt) && !ws.contains(id.rs);
            default:
                return false;
        }

    }

    /**
     * 进入post_buffer
     */
    private static void post_buffer() {


        if(COUNT_ALU>0){
            Introduction it = Pre_ALU_Queue_list.get(0);
            if(it.operate==false){
                prepare_operands(it);
                it.operate = true;
            }
            if( reserves[2].canChangeState && Post_ALU_Buffer==null  && reserves[2].Rj && reserves[2].Rk ) {
                Post_ALU_Buffer = it;
                Pre_ALU_Queue_list.remove(0);
            }

        }
        if(COUNT_ALUB>0){
            Introduction it = Pre_ALUB_Queue_list.get(0);
            System.out.println(it.run_time);
            if(Post_ALUB_Buffer==null && it.operate==false){
                prepare_operands(it);
                it.operate = true;
            }
            if(reserves[3].canChangeState && reserves[3].Rj && reserves[3].Rk  ) {

                if(it.run_time<=2) {
                    it.run_time += 1;
                }

                if(it.run_time==2 && Post_ALUB_Buffer==null){
                    Post_ALUB_Buffer = it;
                    Pre_ALUB_Queue_list.remove(0);
                }
            }

        }
        if(COUNT_MEM>0){
            Introduction it = Pre_MEM_Queue_list.get(0);
            if(it.operate==false){
                prepare_operands(it);
                it.operate = true;
            }
            System.out.println(reserves[1].canChangeState);
            System.out.println(it.name);
            System.out.println(it.name.equals("LW"));
            System.out.println(Post_MEM_Buffer==null);
            System.out.println(reserves[1].Rj );
            System.out.println( reserves[1].Rk);

            if(reserves[1].canChangeState &&  it.name.equals("SW")  && reserves[1].Rj && reserves[1].Rk ){
                excute("MEM");
                Pre_MEM_Queue_list.remove(0);

            }else if(reserves[1].canChangeState &&  it.name.equals("LW")  && Post_MEM_Buffer==null && reserves[1].Rj && reserves[1].Rk) {
                Post_MEM_Buffer = it;
                Pre_MEM_Queue_list.remove(0);
            }
        }
        if(IF_Unit_Wait!=null && IF_Unit_Wait.operate ==true ) {
            if( reserves[0].canChangeState &&  reserves[0].Rj && reserves[0].Rk ){
                IF_Unit_Excute = IF_Unit_Wait;
                IF_Unit_Wait = null;
                if(IF_Unit_Excute.name.equals("BREAK")) finish = false;
            }
        }

    }

    //准备数据保存在保留区
    private static void prepare_operands(Introduction idct) {
        if (idct.name.equals("J")) {
            reserves[0].op = "J";
            reserves[0].Vi = idct.immediate;
            reserves[0].Vj = 0;
            reserves[0].Vk = 0;
            reserves[0].Qj = "";
            reserves[0].Qk = "";
            reserves[0].Rj = true;
            reserves[0].Rk = true;
            reserves[0].A = idct.address;
            IF_Unit_Excute = IF_Unit_Wait;
            IF_Unit_Wait = null;
        }else if (idct.name.equals("JR")) {
            reserves[0].op = "JR";
            reserves[0].Vi = 0;
            if(RegState[idct.rs].equals("")){
                reserves[0].Vj = R[idct.rs];
                reserves[0].Qj = "";
                reserves[0].Rj = true;
            }else{
                reserves[0].Vj = 0;
                reserves[0].Qj = RegState[idct.rs];
                reserves[0].Rj = false;
            }
            reserves[0].Vk = 0;
            reserves[0].Qk = "";
            reserves[0].Rk = true;
            reserves[0].A = idct.address;

        }else if (idct.name.equals("BGTZ")) {
            reserves[0].op = "BGTZ";
            reserves[0].Vi = idct.immediate;
            if(RegState[idct.rs].equals("")){
                reserves[0].Vj = R[idct.rs];
                reserves[0].Qj = "";
                reserves[0].Rj = true;
            }else{
                reserves[0].Vj = 0;
                reserves[0].Qj = RegState[idct.rs];
                reserves[0].Rj = false;
            }
            reserves[0].Vk = 0;
            reserves[0].Qk = "";
            reserves[0].Rk = true;
            reserves[0].A = idct.address;


        }else if (idct.name.equals("BLTZ")) {
            reserves[0].op = "BLTZ";
            reserves[0].Vi = idct.immediate;
            if(RegState[idct.rs].equals("")){
                reserves[0].Vj = R[idct.rs];
                reserves[0].Qj = "";
                reserves[0].Rj = true;
            }else{
                reserves[0].Vj = 0;
                reserves[0].Qj = RegState[idct.rs];
                reserves[0].Rj = false;
            }
            reserves[0].Vk = 0;
            reserves[0].Qk = "";
            reserves[0].Rk = true;
            reserves[0].A = idct.address;

        }else if (idct.name.equals("BEQ")) {
            reserves[0].op = "BEQ";
            reserves[0].Vi = idct.immediate;
            if(RegState[idct.rs].equals("")){
                reserves[0].Vj = R[idct.rs];
                reserves[0].Qj = "";
                reserves[0].Rj = true;
            }else{
                reserves[0].Vj = 0;
                reserves[0].Qj = RegState[idct.rs];
                reserves[0].Rj = false;
            }
            if(RegState[idct.rt].equals("")){
                reserves[0].Vk = R[idct.rt];
                reserves[0].Qk = "";
                reserves[0].Rk = true;
            }else{
                reserves[0].Vk = 0;
                reserves[0].Qk = RegState[idct.rt];
                reserves[0].Rk = false;
            }
            reserves[0].A = idct.address;
        }else if ("ADD,SUB,AND,NOR,SLT".indexOf(idct.name)!=-1) {
            reserves[2].op = idct.name;
            reserves[2].Vi = 0;
            if(RegState[idct.rs].equals("")){
                reserves[2].Vj = R[idct.rs];
                reserves[2].Qj = "";
                reserves[2].Rj = true;
            }else{
                reserves[2].Vj = 0;
                reserves[2].Qj = RegState[idct.rs];
                reserves[2].Rj = false;
            }
            if(RegState[idct.rt].equals("")){
                reserves[2].Vk = R[idct.rt];
                reserves[2].Qk = "";
                reserves[2].Rk = true;
            }else{
                reserves[2].Vk = 0;
                reserves[2].Qk = RegState[idct.rt];
                reserves[2].Rk = false;
            }
            RegState[idct.rd] = "ALU";
            reserves[2].A = idct.address;
        }else if ("ADDI,SUBI,ANDI,NORI,SLTI".indexOf(idct.name)!=-1) {
            reserves[2].op = idct.name;
            reserves[2].Vi = 0;
            if (RegState[idct.rs].equals("")) {
                reserves[2].Vj = R[idct.rs];
                reserves[2].Qj = "";
                reserves[2].Rj = true;
            } else {
                reserves[2].Vj = 0;
                reserves[2].Qj = RegState[idct.rs];
                reserves[2].Rj = false;
            }
            reserves[2].Vk = idct.immediate;
            reserves[2].Qk = "";
            reserves[2].Rk = true;
            reserves[2].A = idct.address;
            RegState[idct.rt] = "ALU";
        }else if ("SRA,SRL,SLL".indexOf(idct.name)!=-1) {
            reserves[3].op = idct.name;
            reserves[3].Vi = 0;
            if (RegState[idct.rt].equals("")) {
                reserves[3].Vj = R[idct.rt];
                reserves[3].Qj = "";
                reserves[3].Rj = true;
            } else {
                reserves[3].Vj = 0;
                reserves[3].Qj = RegState[idct.rt];
                reserves[3].Rj = false;
            }
            reserves[3].Vk = idct.sa;
            reserves[3].Qk = "";
            reserves[3].Rk = true;
            reserves[3].A = idct.address;
            RegState[idct.rd] = "ALUB";
        }else if (idct.name.equals("MUL")) {
            reserves[3].op = idct.name;
            reserves[3].Vi = 0;
            if(RegState[idct.rs].equals("")){
                reserves[3].Vj = R[idct.rs];
                reserves[3].Qj = "";
                reserves[3].Rj = true;
            }else{
                reserves[3].Vj = 0;
                reserves[3].Qj = RegState[idct.rs];
                reserves[3].Rj = false;
            }
            if(RegState[idct.rt].equals("")){
                reserves[3].Vk = R[idct.rt];
                reserves[3].Qk = "";
                reserves[3].Rk = true;
            }else{
                reserves[3].Vk = 0;
                reserves[3].Qk = RegState[idct.rt];
                reserves[3].Rk = false;
            }
            reserves[3].A = idct.address;
            RegState[idct.rd] = "ALUB";
        }else if (idct.name.equals("MULI")) {
            reserves[3].op = idct.name;
            reserves[3].Vi = 0;
            if (RegState[idct.rs].equals("")) {
                reserves[3].Vj = R[idct.rs];
                reserves[3].Qj = "";
                reserves[3].Rj = true;
            } else {
                reserves[3].Vj = 0;
                reserves[3].Qj = RegState[idct.rs];
                reserves[3].Rj = false;
            }
            reserves[3].Vk = idct.immediate;
            reserves[3].Qk = "";
            reserves[3].Rk = true;
            reserves[3].A = idct.address;
            RegState[idct.rt] = "ALUB";
        }else if (idct.name.equals("LW")) {
            reserves[1].op = idct.name;
            reserves[1].Vi = 0;
            if (RegState[idct.rs].equals("")) {
                reserves[1].Vj = R[idct.rs];
                reserves[1].Qj = "";
                reserves[1].Rj = true;
            } else {
                reserves[1].Vj = 0;
                reserves[1].Qj = RegState[idct.rs];
                reserves[1].Rj = false;
            }
            reserves[1].Vk = idct.immediate;
            reserves[1].Qk = "";
            reserves[1].Rk = true;
            reserves[1].A = idct.address;
            RegState[idct.rt] = "MEM";
        }else if (idct.name.equals("SW")) {
            reserves[1].op = idct.name;
            reserves[1].Vi = idct.rt;
            if (RegState[idct.rs].equals("")) {
                reserves[1].Vj = R[idct.rs];
                reserves[1].Qj = "";
                reserves[1].Rj = true;
            } else {
                reserves[1].Vj = 0;
                reserves[1].Qj = RegState[idct.rs];
                reserves[1].Rj = false;
            }
            reserves[1].Vk = idct.immediate;
            reserves[1].Qk = "";
            reserves[1].Rk = true;
            reserves[1].A = idct.address;

        }
    }


    //检测指令是否有WAW
    private static boolean NoWAW(Introduction idct) {

            switch (idct.name){
                case "J":
                    return true;
                case "JR":
                    return RegState[idct.rs].equals("");
                case "ADD":
                    return RegState[idct.rd].equals("");
                case "ADDI":
                    return RegState[idct.rt].equals("");
                case "NOR":
                    return RegState[idct.rd].equals("");
                case "NORI":
                    return RegState[idct.rt].equals("");
                case "SLT":
                    return RegState[idct.rd].equals("");
                case "SLTI":
                    return RegState[idct.rt].equals("");
                case "SUB":
                    return RegState[idct.rd].equals("");
                case "SUBI":
                    return RegState[idct.rt].equals("");
                case "SLL":
                    return RegState[idct.rd].equals("");
                case "SRL":
                    return RegState[idct.rd].equals("");
                case "SRA":
                    return RegState[idct.rd].equals("");
                case "MUL":
                    return RegState[idct.rd].equals("");
                case "MULI":
                    return RegState[idct.rt].equals("");
                case "LW":
                    return RegState[idct.rt].equals("");
                case "SW":
                    return true;
                case "BGTZ":
                    return true;
                case "BLTZ":
                    return true;
                case "BEQ":
                    return true;
                case "AND":
                    return RegState[idct.rd].equals("");
                case "ANDI":
                    return RegState[idct.rt].equals("");

            }
            return true;
        }


    //检测指令是否有WAR
    private static boolean NoWAR(Introduction idct) {

        switch (idct.name){
            case "J":
                return true;
            case "JR":
                return RegState[idct.rs].equals("")  ;
            case "ADD":
                return RegState[idct.rd].equals("") && RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "ADDI":
                return RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "NOR":
                return RegState[idct.rd].equals("") && RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "NORI":
                return RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "SLT":
                return RegState[idct.rd].equals("") && RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "SLTI":
                return RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "SUB":
                return RegState[idct.rd].equals("") && RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "SUBI":
                return RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "SLL":
                return RegState[idct.rd].equals("") && RegState[idct.rt].equals("");
            case "SRL":
                return RegState[idct.rd].equals("") && RegState[idct.rt].equals("");
            case "SRA":
                return RegState[idct.rd].equals("") && RegState[idct.rt].equals("");
            case "MUL":
                return RegState[idct.rd].equals("") && RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "MULI":
                return RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "LW":
                return RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "SW":
                return RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "BGTZ":
                return RegState[idct.rs].equals("");
            case "BLTZ":
                return RegState[idct.rs].equals("");
            case "BEQ":
                return RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "AND":
                return RegState[idct.rd].equals("") && RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
            case "ANDI":
                return RegState[idct.rt].equals("") && RegState[idct.rs].equals("");
        }
        return true;
    }

    /**
     * 检测指令类型
     * @param it
     * @return
     */
    private static boolean isALUB(Introduction it) {
        return ALUBInstruction.indexOf(it.name)!=-1;
    }

    private static boolean isMEM(Introduction it) {
        return MEMInstruction.indexOf(it.name)!=-1;
    }

    public static boolean isALU(Introduction it){
        return ALUInstruction.indexOf(it.name)!=-1;
    }

    private static boolean isBranch(Introduction it) {
        return BranchInstruction.indexOf(it.name)!=-1;
    }


    /**
     * 取指令
     */
    private static void instruct_fetch() {
        int size = Pre_Issue_Buffer_list.size();
        for (int i = size;i < 4 && op_cnt < 2; i++ ){
            System.out.println(PC);
            if(isBranch(its.get(PC))) {
                Introduction it = its.get(PC);
                if( it.name.equals("J")){
                    prepare_operands(it);
                    IF_Unit_Excute = it; //96-64
                    IF_Unit_Excute.operate = true;
                    canFetch = 1;
                    PC++;
                    if(its.get(PC).name.equals("BREAK")){
                        IF_Unit_Excute = its.get(PC); //96-64
                        IF_Unit_Excute.operate = true;
                        canFetch = 1;
                        PC = -1 ;
                    }
                }else if( it.name.equals("BREAK")){
                    IF_Unit_Excute = it; //96-64
                    IF_Unit_Excute.operate = true;
                    canFetch = 1;
                    PC=-1;
                }else if( it.name.equals("NOP")){
                    IF_Unit_Excute = it; //96-64
                    IF_Unit_Excute.operate = true;
                    PC ++;
                    op_cnt ++ ;
                    continue;
                }
                else {
                    IF_Unit_Wait = it;
                    IF_Unit_Wait.operate = false;
                    canFetch = 1;
                    PC++;
                    if(its.get(PC).name.equals("BREAK")){
                        IF_Unit_Excute = its.get(PC); //96-64
                        IF_Unit_Excute.operate = true;
                        canFetch = 1;
                        PC = -1 ;
                    }
                }

                return;
            }
            else {
                Introduction introduction = its.get(PC);
                introduction.operate = false;
                Pre_Issue_Buffer_list.add(introduction);
                PC++;
            }
            op_cnt ++ ;
        }

    }



    /**
     * 读取二进制文件
     * @return
     * @throws Exception
     */
    private static List<String> loadFiles() throws Exception {
        BufferedReader br =new BufferedReader(new FileReader(new File(InputFilePath)));
        List<String> ls = new ArrayList<>();
        String line = "";

        while ((line = br.readLine())!=null){
            ls.add(line.trim());
        }
        br.close();

        return ls ;
    }


    /**
     * 反汇编
     * @param itr
     * @param count
     * @param bw1
     * @return
     * @throws IOException
     */
    public static String disassembly (String itr, int count, BufferedWriter bw1) throws IOException {

        String start = itr.substring(0,6);
        String function = itr.substring(26,32);
        String fstr = format(itr);
        String rt = itr.substring(11,16);
        String rs = itr.substring(6,11);
        String rd = itr.substring(16,21);
        String sa = itr.substring(21,26);

        String it = null;

        if(finished) {
            Data data = new Data();
            data.address = count*4 + 64 ;
            data.data =  Integer.parseInt(FromHexToStrTen(itr));
            datas.add(data);
            it = itr +"\t"+ (count * 4 + 64) + "\t" + FromHexToStrTen(itr);

        }else {
            Introduction idct = new Introduction();
            idct.detail = itr;
            idct.address = count * 4 + 64 ;
            idct.rs = Integer.parseInt(FromHexToStrTen(rs));
            idct.rd = Integer.parseInt(FromHexToStrTen(rd));
            idct.rt = Integer.parseInt(FromHexToStrTen(rt));
            idct.sa = Integer.parseInt(FromHexToStrTen(sa));
            // J
            if(start.equals("000010")) {
                idct.name = "J";
                idct.immediate = Integer.valueOf(itr.substring(6), 2) * 4;
                idct.estr ="J\t#" + (Integer.valueOf(itr.substring(6), 2) * 4);
            }
            //special δ  JR ADD AND NOR SLT SUB BREAK SLL SRL SRA
            else if (start.equals("000000")) {
                // NOP
                if (itr.equals("00000000000000000000000000000000")) {
                    idct.name = "NOP";
                    idct.estr ="NOP";
                }
                // JR
                else if (function.equals("001000")) {
                    idct.name = "JR";
                    idct.estr ="JR\tR" + FromHexToStrTen(rs);
                }
                // ADD
                else if (function.equals("100000")) {
                    idct.name = "ADD";
                    idct.estr ="ADD\tR" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

                }
                // AND
                else if (start.equals("100100")) {
                    idct.name = "AND";
                    idct.estr ="AND\tR" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

                }
                // NOR
                else if (start.equals("100111")) {
                    idct.name = "NOR";
                    idct.estr ="NOR\tR" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

                }
                // SLT
                else if (start.equals("101010")) {
                    idct.name = "SLT";
                    idct.estr ="SLT\tR" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

                }
                // SUB
                else if (function.equals("100010")) {
                    idct.name = "SUB";
                    idct.estr ="SUB\tR" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

                }
                // BREAK
                else if (function.equals("001101")) {
                    idct.name = "BREAK";
                    idct.estr ="BREAK";
                    finished = true;
                }

                // SLL
                else if (function.equals("000000")) {
                    //return "SLL";
                    idct.name = "SLL";
                    idct.estr ="SLL\tR" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rt) + ", #" + FromHexToStrTen(sa);
                }
                // SRL
                else if (function.equals("000010")) {
                    idct.name = "SRL";
                    idct.estr ="SRL\tR" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rt) + ", #" + FromHexToStrTen(sa);

                }
                // SRA
                else if (function.equals("000011")) {
                    idct.name = "SRA";
                    idct.estr ="SRA\tR" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rt) + ", #" + FromHexToStrTen(sa);
                }
            }
            // MUL
            else if (start.equals("011100")) {
                if (function.equals("000010")) {
                    idct.name = "MUL";
                    idct.estr ="MUL\tR" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);
                }
            }
            //regimm δ   BLTZ
            else if (start.equals("000001")) {
                // BLTZ
                if (rt.equals("00000")) {
                    idct.name = "BLTZ";
                    idct.immediate = Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4;
                    idct.estr ="BLTZ\tR" + FromHexToStrTen(rs) + ", #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4);
                }
            }
            //BGTZ
            else if (start.equals("000111")) {
                idct.name = "BGTZ";
                idct.immediate = Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4;

                idct.estr ="BGTZ\tR" + FromHexToStrTen(rs) + ", #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4);
            }

            //BEQ
            else if (start.equals("000100")) {
                idct.name = "BEQ";
                idct.immediate = Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4;
                idct.estr ="BEQ\tR" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt) + ", #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4);
            }


            //LW
            else if (start.equals("100011")) {
                idct.name = "LW";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="LW\tR" + FromHexToStrTen(rt) + ", " + (Integer.parseInt(FromHexToStrTen(itr.substring(16)))) + "(R" + FromHexToStrTen(rs) + ")";

            }
            //SW
            else if (start.equals("101011")) {
                idct.name = "SW";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="SW\tR" + FromHexToStrTen(rt) + ", " + (Integer.parseInt(FromHexToStrTen(itr.substring(16)))) + "(R" + FromHexToStrTen(rs) + ")";
            }

            // ADDI
            else if (start.equals("110000")) {
                idct.name = "ADDI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="ADD\tR" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + ", #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
            }

            // SUBI
            else if (start.equals("110001")) {
                idct.name = "SUBI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="SUB\tR" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
            }

            // MULI
            else if (start.equals("100001")) {
                idct.name = "MULI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="MUL\tR" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));

            }
            // ANDI
            else if (start.equals("110010")) {
                idct.name = "ANDI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="AND\tR" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));

            }


            // NORI
            else if (start.equals("110011")) {
                idct.name = "NORI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="NOR\tR" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));

            }

            // SLTI
            else if (start.equals("110101")) {
                idct.name = "SLTI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="SLT\tR" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));

            }
            it = fstr + (count * 4 + 64) + "\t" + idct.estr;
            its.add(idct);

        }
        System.out.println(it);
        if (count==0)
            bw1.write(it);
        else {
            bw1.newLine();
            bw1.write(it);
        }
        return it;
    }

    /**
     * 二进制转十进制
     * @param rs
     * @return
     */
    private static String FromHexToStrTen(String rs) {

        if(rs.length() == 32){
            if(rs.startsWith("1")) return "-"+((long)(Math.pow(2,32)-Long.valueOf(rs,2)));
        }
        return Long.valueOf(rs,2).toString();
    }

    /**
     * 格式化输入
     * @param itr
     * @return
     */
    private static String format(String itr) {
        return itr.substring(0,1)+" "+itr.substring(1,6)+" "+itr.substring(6,11)+" "+itr.substring(11,16)+" "+itr.substring(16,21)+" "+itr.substring(21,26)+" "+ itr.substring(26)+"\t";
    }


    /**
     * 输出寄存器状态
     * @param bw2
     * @throws IOException
     */
    private static void printRegs(BufferedWriter bw2) throws IOException {

        System.out.println();
        System.out.println("Registers");
        bw2.newLine();
        bw2.write("Registers");
        bw2.newLine();
        System.out.print("R00:\t");
        bw2.write("R00:\t");
        for (int i = 0 ; i < 8 ;i++) {
            if(i==7){
                System.out.print(R[i]);
                bw2.write(R[i]+"");
            }else {
                System.out.print(R[i] + "\t");
                bw2.write(R[i] + "\t");
            }
        }
        System.out.println();
        bw2.newLine();
        System.out.print("R08:\t");
        bw2.write("R08:\t");
        for (int i = 8 ; i < 16 ;i++) {
            if(i==15){
                System.out.print(R[i]);
                bw2.write(R[i]+"");
            }else {
                System.out.print(R[i] + "\t");
                bw2.write(R[i] + "\t");
            }
        }
        System.out.println();
        bw2.newLine();
        System.out.print("R16:\t");
        bw2.write("R16:\t");
        for (int i = 16 ; i < 24 ;i++) {
            if(i==23){
                System.out.print(R[i]);
                bw2.write(R[i]+"");
            }else {
                System.out.print(R[i] + "\t");
                bw2.write(R[i] + "\t");
            }
        }
        System.out.println();
        bw2.newLine();
        System.out.print("R24:\t");
        bw2.write("R24:\t");
        for (int i = 24 ; i < 32 ;i++) {
            if(i==31){
                System.out.print(R[i]);
                bw2.write(R[i]+"");
            }else {
                System.out.print(R[i] + "\t");
                bw2.write(R[i] + "\t");
            }
        }
        System.out.println();
        bw2.newLine();
    }

    /**
     * 输出数据状态
     * @param bw2
     * @throws IOException
     */
    private static void printDatas(BufferedWriter bw2) throws IOException {
        System.out.println();
        System.out.println("Data");
        bw2.newLine();
        bw2.write("Data");
        bw2.newLine();
        int n = 0 ;
        for (Data i:datas) {
            if(n%8==0) {
                if (n != 0)
                {
                    System.out.println();
                    bw2.newLine();
                }
                System.out.print(i.address+":\t");
                bw2.write(i.address+":\t");
            }

            if(n%8 == 7){
                System.out.print(i.data+"");
                bw2.write(i.data+"");
            }else{
                System.out.print(i.data+"\t");
                bw2.write(i.data+"\t");
            }


            n++;
        }
        System.out.println();
        bw2.newLine();
    }


    /**
     * 输出其他数据【pre_queue.....】
     * @param bw2
     * @throws IOException
     */
    private static void printBuffer(BufferedWriter bw2) throws IOException {
        System.out.println();
        bw2.newLine();
        bw2.newLine();
        System.out.println("IF Unit:");
        bw2.write("IF Unit:\n");
        if(IF_Unit_Wait!=null){
            System.out.println("\tWaiting Instruction: "+IF_Unit_Wait.estr+"");
            bw2.write("\tWaiting Instruction: "+IF_Unit_Wait.estr+"\n");
        }else{
            System.out.println("\tWaiting Instruction: ");
            bw2.write("\tWaiting Instruction: \n");
        }
        if(IF_Unit_Excute!=null){
            System.out.println("\tExecuted Instruction: "+IF_Unit_Excute.estr+"");
            bw2.write("\tExecuted Instruction: "+IF_Unit_Excute.estr+"\n");
        }else{
            System.out.println("\tExecuted Instruction: ");
            bw2.write("\tExecuted Instruction: \n");
        }

        System.out.println("Pre-Issue Buffer:");
        bw2.write("Pre-Issue Buffer:\n");
        int i = 0 ;
        for (Introduction it: Pre_Issue_Buffer_list) {
            System.out.println("\tEntry "+i+":["+it.estr+"]");
            bw2.write("\tEntry "+i+":["+it.estr+"]\n");
            i++;
        }
        while(i<4){
            System.out.println("\tEntry "+i+":");
            bw2.write("\tEntry "+i+":\n");
            i++;
        }


        System.out.println("Pre-ALU Queue:");
        bw2.write("Pre-ALU Queue:\n");
        i = 0 ;
        for (Introduction it: Pre_ALU_Queue_list) {
            System.out.println("\tEntry "+i+":["+it.estr+"]");
            bw2.write("\tEntry "+i+":["+it.estr+"]\n");
            i++;
        }
        while(i<2){
            System.out.println("\tEntry "+i+":");
            bw2.write("\tEntry "+i+":\n");
            i++;
        }
        if(Post_ALU_Buffer!=null) {
            System.out.println("Post-ALU Buffer:[" + Post_ALU_Buffer.estr+"]");
            bw2.write("Post-ALU Buffer:[" + Post_ALU_Buffer.estr + "]\n");
        }else{
            System.out.println("Post-ALU Buffer:" );
            bw2.write("Post-ALU Buffer:\n");
        }

        System.out.println("Pre-ALUB Queue:");
        bw2.write("Pre-ALUB Queue:\n");
        i = 0 ;
        for (Introduction it: Pre_ALUB_Queue_list) {
            System.out.println("\tEntry "+i+":["+it.estr+"]");
            bw2.write("\tEntry "+i+":["+it.estr+"]\n");
            i++;
        }
        while(i<2){
            System.out.println("\tEntry "+i+":");
            bw2.write("\tEntry "+i+":\n");
            i++;
        }
        if(Post_ALUB_Buffer!=null) {
            System.out.println("Post-ALUB Buffer:[" + Post_ALUB_Buffer.estr+"]");
            bw2.write("Post-ALUB Buffer:[" + Post_ALUB_Buffer.estr + "]\n");
        }else{
            System.out.println("Post-ALUB Buffer:" );
            bw2.write("Post-ALUB Buffer:\n");
        }


        System.out.println("Pre-MEM Queue:");
        bw2.write("Pre-MEM Queue:\n");
        i = 0 ;
        for (Introduction it: Pre_MEM_Queue_list) {
            System.out.println("\tEntry "+i+":["+it.estr+"]");
            bw2.write("\tEntry "+i+":["+it.estr+"]\n");
            i++;
        }
        while(i<2){
            System.out.println("\tEntry "+i+":");
            bw2.write("\tEntry "+i+":\n");
            i++;
        }
        if(Post_MEM_Buffer!=null) {
            System.out.println("Post-MEM Buffer:[" + Post_MEM_Buffer.estr+"]");
            bw2.write("Post-MEM Buffer:[" + Post_MEM_Buffer.estr + "]\n");
        }else{
            System.out.println("Post-MEM Buffer:" );
            bw2.write("Post-MEM Buffer:\n");
        }

    }

}