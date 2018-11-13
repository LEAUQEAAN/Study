import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**********************************************************
 *  时间：2018-11-13                                      *
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

    //二进制的输入文件路径
    static String InputFilePath = "sample.txt";

    //反汇编的结果文件路径
    static String OuputFilePath1 = "disassembly.txt";

    //执行结果文件路径
    static String OuputFilePath2 = "simulation.txt";

    //标志是否编译完了
    static Boolean finished = false ;

    //程序计数器，存放的是当前执行的命令的下标
    static int PC = 0 ;

    //寄存器
    static int[] R = new int[32];

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
    }

    public static void main(String[] args) throws Exception {


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

        for (PC = 0 ; PC < its.size() ;PC++) {
            Introduction idct = its.get(PC) ;
            System.out.println("--------------------\nCycle:" + (count+1) + "\t" + idct.address + "\t" + idct.estr);
            bw2.write("--------------------\nCycle:" + (count+1) + "\t" + idct.address + "\t" + idct.estr);
            simulation(idct);
            printRegs(bw2);
            printDatas(bw2);
            System.out.println();
            bw2.newLine();
            count++;
        }
        bw2.close();

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
                idct.estr ="J #" + (Integer.valueOf(itr.substring(6), 2) * 4);
            }
            //special δ  JR ADD AND NOR SLT SUB BREAK SLL SRL SRA
            else if (start.equals("000000")) {
                // JR
                if (function.equals("001000")) {
                    idct.name = "JR";
                    idct.estr ="JR R" + FromHexToStrTen(rs);
                }
                // ADD
                else if (function.equals("100000")) {
                    idct.name = "ADD";
                    idct.estr ="ADD R" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

                }
                // AND
                else if (start.equals("100100")) {
                    idct.name = "AND";
                    idct.estr ="AND R" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

                }
                // NOR
                else if (start.equals("100111")) {
                    idct.name = "NOR";
                    idct.estr ="NOR R" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

                }
                // SLT
                else if (start.equals("101010")) {
                    idct.name = "SLT";
                    idct.estr ="SLT R" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

                }
                // SUB
                else if (function.equals("100010")) {
                    idct.name = "SUB";
                    idct.estr ="SUB R" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);

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
                    idct.estr ="SLL R" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rt) + ", #" + FromHexToStrTen(sa);
                }
                // SRL
                else if (function.equals("000010")) {
                    idct.name = "SRL";
                    idct.estr ="SRL R" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rt) + ", #" + FromHexToStrTen(sa);

                }
                // SRA
                else if (function.equals("000011")) {
                    idct.name = "SRA";
                    idct.estr ="SRA R" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rt) + ", #" + FromHexToStrTen(sa);
                }
            }
            // MUL
            else if (start.equals("011100")) {
                if (function.equals("000010")) {
                    idct.name = "MUL";
                    idct.estr ="MUL R" + FromHexToStrTen(rd) + ", R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt);
                }
            }
            //regimm δ   BLTZ
            else if (start.equals("000001")) {
                // BLTZ
                if (rt.equals("00000")) {
                    idct.name = "BLTZ";
                    idct.immediate = Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4;
                    idct.estr ="BLTZ R" + FromHexToStrTen(rs) + ", #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4);
                }
            }
            //BGTZ
            else if (start.equals("000111")) {
                idct.name = "BGTZ";
                idct.immediate = Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4;

                idct.estr ="BGTZ R" + FromHexToStrTen(rs) + ", #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4);
            }

            //BEQ
            else if (start.equals("000100")) {
                idct.name = "BEQ";
                idct.immediate = Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4;
                idct.estr ="BEQ R" + FromHexToStrTen(rs) + ", R" + FromHexToStrTen(rt) + ", #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))) * 4);
            }


            //LW
            else if (start.equals("100011")) {
                idct.name = "LW";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="LW R" + FromHexToStrTen(rt) + ", " + (Integer.parseInt(FromHexToStrTen(itr.substring(16)))) + "(R" + FromHexToStrTen(rs) + ")";

            }
            //SW
            else if (start.equals("101011")) {
                idct.name = "SW";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="SW R" + FromHexToStrTen(rt) + ", " + (Integer.parseInt(FromHexToStrTen(itr.substring(16)))) + "(R" + FromHexToStrTen(rs) + ")";
            }

            // ADDI
            else if (start.equals("110000")) {
                idct.name = "ADDI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="ADD R" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + ", #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
            }

            // SUBI
            else if (start.equals("110001")) {
                idct.name = "SUBI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="SUB R" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
            }

            // MULI
            else if (start.equals("100001")) {
                idct.name = "MULI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="MUL R" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));

            }
            // ANDI
            else if (start.equals("110010")) {
                idct.name = "ANDI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="AND R" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));

            }


            // NORI
            else if (start.equals("110011")) {
                idct.name = "NORI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="NOR R" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));

            }

            // SLTI
            else if (start.equals("110101")) {
                idct.name = "SLTI";
                idct.immediate = (Integer.parseInt(FromHexToStrTen(itr.substring(16))));
                idct.estr ="SLT R" + FromHexToStrTen(rt) + ", R" + FromHexToStrTen(rs) + "  #" + (Integer.parseInt(FromHexToStrTen(itr.substring(16))));

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
        return itr.substring(0,6)+" "+itr.substring(6,11)+" "+itr.substring(11,16)+" "+itr.substring(16,21)+" "+itr.substring(21,26)+" "+ itr.substring(26)+"\t";
    }

    /**
     * 输出寄存器状态
     * @param bw2
     * @throws IOException
     */
    private static void printRegs(BufferedWriter bw2) throws IOException {
        System.out.println();
        bw2.newLine();
        System.out.println("Registers");
        bw2.newLine();
        bw2.write("Registers");
        bw2.newLine();
        System.out.print("R00:\t");
        bw2.write("R00:\t");
        for (int i = 0 ; i < 16 ;i++) {
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
        for (int i = 16 ; i < 32 ;i++) {
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


    /**
     * 运行汇编语言
     * @param idct
     */
    public static void simulation(Introduction idct){

        switch (idct.name){

            case "J":
                J(idct);
                break;
            case "JR":
                JR(idct);
                break;
            case "ADD":
                ADD(idct);
                break;
            case "ADDI":
                ADDI(idct);
                break;
            case "NOR":
                NOR(idct);
                break;
            case "NORI":
                NORI(idct);
                break;
            case "SLT":
                SLT(idct);
                break;
            case "SLTI":
                SLTI(idct);
                break;
            case "SUB":
                SUB(idct);
                break;
            case "SUBI":
                SUBI(idct);
                break;
            case "SLL":
                SLL(idct);
                break;
            case "SRL":
                SRL(idct);
                break;
            case "SRA":
                SRA(idct);
                break;
            case "MUL":
                MUL(idct);
                break;
            case "MULI":
                MULI(idct);
                break;
            case "LW":
                LW(idct);
                break;
            case "SW":
                SW(idct);
                break;
            case "BGTZ":
                BGTZ(idct);
                break;
            case "BLTZ":
                BLTZ(idct);
                break;
            case "BEQ":
                BEQ(idct);
                break;
            case "AND":
                AND(idct);
                break;
            case "ANDI":
                ANDI(idct);
                break;
        }


    }

    /**
     * 立即数与
     * @param idct
     */
    private static void ANDI(Introduction idct) {
        R[idct.rt] = R[idct.rs]  & idct.immediate;
    }

    /**
     * 寄存器与
     * @param idct
     */
    private static void AND(Introduction idct) {
        R[idct.rd] = R[idct.rs]  & R[idct.rt] ;
    }

    /**
     * 条件等于跳转
     * @param idct
     */
    private static void BEQ(Introduction idct) {
        if(R[idct.rt] == R[idct.rs]){
            PC = PC + idct.immediate/4;
        }
    }

    /**
     * 条件小于跳转
     * @param idct
     */
    private static void BLTZ(Introduction idct) {
        if(R[idct.rs] < 0){
            PC =  PC + idct.immediate/4;
        }
    }

    /**
     * 条件大于跳转
     * @param idct
     */
    private static void BGTZ(Introduction idct) {
        if(R[idct.rs] > 0){
            PC = PC + idct.immediate/4;
        }
    }

    /**
     * 存入内存
     * @param idct
     */
    private static void SW(Introduction idct) {
        for(int i=0;i< datas.size() ;i++ ){
            Data data = datas.get(i);
            if (data.address == (R[idct.rs]+ idct.immediate)) {
                data.data = R[idct.rt];
                datas.set(i,data);
                return;
            }
        }
    }

    /**
     * 导入到寄存器
     * @param idct
     */
    private static void LW(Introduction idct) {
        for(int i=0;i< datas.size() ;i++ ){
            Data data = datas.get(i);
            if (data.address == (R[idct.rs]+ idct.immediate)) {
                R[idct.rt] = data.data;
                return;
            }
        }
    }

    /**
     * 立即数乘法
     * @param idct
     */
    private static void MULI(Introduction idct) {
        R[idct.rt] = R[idct.rs] * idct.immediate ;
    }

    /**
     * 乘法
     * @param idct
     */
    private static void MUL(Introduction idct) {
        R[idct.rd] = R[idct.rs] * R[idct.rt] ;
    }

    /**
     * 算术右移
     * @param idct
     */
    private static void SRA(Introduction idct) {
        R[idct.rd] = R[idct.rt] >> idct.sa ;
    }

    /**
     * 逻辑右移
     * @param idct
     */
    private static void SRL(Introduction idct) {
        R[idct.rd] = R[idct.rt] >>> idct.sa;

    }

    /**
     * 左移
     * @param idct
     */
    private static void SLL(Introduction idct) {
        R[idct.rd] = R[idct.rt] << idct.sa;
    }

    /**
     * 立即数减法
     * @param idct
     */
    private static void SUBI(Introduction idct) {
        R[idct.rt] = R[idct.rs] - idct.immediate ;
    }

    /**
     * 减法
     * @param idct
     */
    private static void SUB(Introduction idct) {
        R[idct.rd] = R[idct.rs] - R[idct.rt] ;
    }


    /**
     * 立即数小于
     * @param idct
     */
    private static void SLTI(Introduction idct) {
        R[idct.rt] = R[idct.rs] < idct.immediate ? 1 : 0;
    }

    /**
     * 小于
     * @param idct
     */
    private static void SLT(Introduction idct) {
        R[idct.rd] = R[idct.rs] < R[idct.rt] ? 1:0 ;
    }

    /**
     * 立即数异或
     * @param idct
     */
    private static void NORI(Introduction idct) {
        R[idct.rt] = R[idct.rs] ^ idct.immediate;
    }

    /**
     * 异或
     * @param idct
     */
    private static void NOR(Introduction idct) {
        R[idct.rd] = R[idct.rs] ^ R[idct.rt];
    }

    /**
     * 立即数加法
     * @param idct
     */
    private static void ADDI(Introduction idct) {
        R[idct.rt] = R[idct.rs] + idct.immediate;
    }

    /**
     * 加法
     * @param idct
     */
    private static void ADD(Introduction idct) {
        R[idct.rd] = R[idct.rs] + R[idct.rt];
    }

    /**
     * 寄存器地址跳转
     * @param idct
     */
    private static void JR(Introduction idct) {
        PC = getIndexByAddress(R[idct.rs]) - 1  ;
    }

    /**
     * 地址跳转
     * @param idct
     */
    private static void J(Introduction idct) {
        PC = getIndexByAddress(idct.immediate) - 1 ;
    }
}
