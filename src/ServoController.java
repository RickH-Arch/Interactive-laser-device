import processing.core.PApplet;
import processing.core.PVector;
import processing.serial.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import hypermedia.net.*;

public class ServoController extends PApplet {
    public static void main(String[] args){

        PApplet.main("ServoController");
        NameInput nameInput = new NameInput();
        Thread t = new Thread(nameInput);
        //t.start();
    }

    UDP servo_udp;
    UDP relay_udp;

    static String relayip = "192.168.2.111";
    static String servoip = "192.168.2.112";
    int udp_port = 9876;


    PVector SETTING_COR;

    public static boolean debugMode = true;

    public static boolean[] HEAD_ACTIVATION = new boolean[]{false,false,false,true,false,false,false,false};
    //public static boolean[] HEAD_ACTIVATION = new boolean[]{true,false,true,true,true,false,true,true};
    //static boolean[] HEAD_BUFFER_STATUS  = new boolean[]{false,false,false,false,false,false,false,false};
    static boolean[] HEAD_BUFFER_STATUS  = new boolean[]{true,true,true,true,true,true,true,true};
    static String[] HEAD_BUFFER_STATUS_String = new String[]{"0","0","0","1","0","0","0","0"};
    String toArduino;
    String toArduinoPre;

    public static int HeadNum = 8;//舵机数量
    String speed = "20";//舵机运动速度
    Serial port;
    Serial arduinoPort;

    float x_magic = (1/3.2f)*2.60f;
    float y_magic = 1.3f/1.1f;



    int switchTime = 2000;//模式切换速度
    int switchDelayTime = 1000;//云台初始化延时
    int nameTime = 2000;//姓名展示时间

    public static int shuffleBuffer = 100;
    ////////////////////////////////////////////////////
    float BASE_INTERVAL = 60;//投射装置间距

    //ORI_COR为X坐标最小（即最边上）的激光装置坐标
    float BASE_Z_ORI_COR = 3300;
    float BASE_X_ORI_COR = 2100;
    float BASE_Y_ORI_COR = 2500;

    //坐标修正（单位：角度）
    PVector[] correction = new PVector[]{
            new PVector( -10,0),//0号云台
            new PVector(-5,30),//1号云台
            new PVector(35,-20),//2号云台
            new PVector(-15,-15),//3号云台
            new PVector(-8,-11),//4号云台
            new PVector(10,10),//5号云台
            new PVector(-20,-25),//6号云台
            new PVector(-10,28),//7号云台
    };



    ////////////////////////////////////////////////////
    boolean ANI_MODE = true;
    boolean shuffleMode = true;
    Wave[] shufflePool;
    Wave queueWave;
    boolean queueMode = false;

    boolean NAME_MODE = false;

    ArrayList<Wave> waves = new ArrayList<Wave>();

    int waveNum;

    Head[] heads;
    int AniTime = 1;
    int NameTime = 1;
    String[] NAMES;
    PVector[] NAMES_COR;

    String preName = "-";
    public static String nameNow = "-";
    PVector nameCorNow;
    ArrayList<String> NAMES_ARRAY;

    boolean initiated = false;


    Scanner scan;

    ArrayList<String> NAME_arr;

    public void setup(){
        //SETTING_COR = new PVector(0*x_magic,0*y_magic);
        scan = new Scanner(System.in);

        servo_udp = new UDP(this,6100);
        relay_udp = new UDP(this,6102);

        port = new Serial(this,"COM6",9600);
        arduinoPort = new Serial(this,"COM4",9600);

        //read name
        NAMES = loadStrings("D:\\_EVENTS_\\________\\无锡会议签到板\\参会姓名新.txt");
        println("Num of participants:"+NAMES.length);
        println(NAMES);
        NAMES_ARRAY = new ArrayList(Arrays.asList(NAMES));



        //read name coordinates
        String[] nameCors = loadStrings("D:\\_EVENTS_\\________\\无锡会议签到板\\姓名坐标新.txt");
        NAMES_COR  = new PVector[nameCors.length];
        for(int i = 0;i<nameCors.length;i++){
            String[] cord = nameCors[i].split(",");
            NAMES_COR[i] = new PVector(Float.parseFloat(cord[0]),Float.parseFloat(cord[1]));
        }
        println("Num of Participants coordinate:"+NAMES_COR.length);

        //read tracks
        //String[] lines = loadStrings("D:\\_EVENTS_\\________\\无锡会议签到板\\轨迹.txt");
        String[] lines = loadStrings("D:\\_EVENTS_\\________\\无锡会议签到板\\轨迹.txt");
        waveNum = lines.length;
        println("waveNum:"+waveNum);

        for(int i = 0;i<lines.length;i++){
            waves.add(new Wave(lines[i],i));
        }

        println(waves.get(0).pts);



        //set heads
        heads = new Head[HeadNum];
        for(int i = 0;i<HeadNum;i++){
            heads[i] = new Head(i,i*2,i*2+1,speed,new PVector(BASE_X_ORI_COR+(i*BASE_INTERVAL),BASE_Y_ORI_COR,BASE_Z_ORI_COR));
            //set correction
            heads[i].SetCorrectAngle(correction[i].x,correction[i].y);
        }

        //set shufflePool
        shufflePool = new Wave[HeadNum];
        for(int i = 0;i<HeadNum;i++){
            int n = (int)random(waveNum);
            waves.get(n).status = true;
            shufflePool[i] = waves.get(n);
            heads[i].InitiateWave(waves.get(n));
        }

        //set queueWave
        int n = (int) random(waveNum);
        queueWave = waves.get(n);

        toArduino = String.join("",HEAD_BUFFER_STATUS_String);
        if(!toArduino.equals(toArduinoPre)){
            toArduinoPre = toArduino;
            arduinoPort.write(toArduino);
            relay_udp.send("t"+toArduino,relayip,9876);
            println(toArduino);
        }


        NAME_arr = new ArrayList<String>(List.of(NAMES));
        relay_udp.send("t"+toArduino,relayip,9876);

    }

    Boolean automode = false;

    public void draw(){
        if (!automode) {


            if (SETTING_COR != null) {
                InitiateHead(SETTING_COR);
                return;
            }
            InitiateHead(new PVector(0, 0));
            HEAD_BUFFER_STATUS_String = new String[]{"0", "0", "0", "0", "0", "0", "0", "0"};
            toArduino = String.join("", HEAD_BUFFER_STATUS_String);
            relay_udp.send("t" + toArduino, relayip, 9876);

            relay_udp.send("t" + toArduino, relayip, 9876);
            String name = scan.nextLine();
            println("=====>" + name);
            HEAD_BUFFER_STATUS_String = new String[]{"0", "0", "0", "1", "0", "0", "0", "0"};
            toArduino = String.join("", HEAD_BUFFER_STATUS_String);
            relay_udp.send("t" + toArduino, relayip, 9876);


            int index = NAME_arr.indexOf(name);
            if (index == -1) {
                print("no such name");
                return;

            }
            PVector target = new PVector(NAMES_COR[index].x * x_magic, NAMES_COR[index].y * y_magic);
            InitiateHead(target);

            delay(20000);


            for (int i = 0; i < 5; i++) {

                for (int j = 0; j < 8; j++) {
                    float ran = random(1);
                    if (ran > 0.5) {
                        HEAD_BUFFER_STATUS_String[j] = "0";
                    } else {
                        HEAD_BUFFER_STATUS_String[j] = "1";
                    }

                }
                HEAD_BUFFER_STATUS_String[3] = "1";

                toArduino = String.join("", HEAD_BUFFER_STATUS_String);

                relay_udp.send("t" + toArduino, relayip, 9876);

                delay(300);
            }

            HEAD_BUFFER_STATUS_String = new String[]{"0", "0", "0", "1", "0", "0", "0", "0"};
            toArduino = String.join("", HEAD_BUFFER_STATUS_String);

            relay_udp.send("t" + toArduino, relayip, 9876);

            delay(1000);

            return;
        }else{

            if (SETTING_COR != null) {
                InitiateHead(SETTING_COR);
                return;
            }

            //InitiateHead(new PVector(0, 0));
            //HEAD_BUFFER_STATUS_String = new String[]{"0", "0", "0", "0", "0", "0", "0", "0"};
            //toArduino = String.join("", HEAD_BUFFER_STATUS_String);
            //relay_udp.send("t" + toArduino, relayip, 9876);

            //relay_udp.send("t" + toArduino, relayip, 9876);

            int index = (int)(random(NAME_arr.size()));

            PVector target = new PVector(NAMES_COR[index].x * x_magic, NAMES_COR[index].y * y_magic);
            InitiateHead(target);

            delay(2000);

            for (int i = 0; i < 5; i++) {

                for (int j = 0; j < 8; j++) {
                    float ran = random(1);
                    if (ran > 0.5) {
                        HEAD_BUFFER_STATUS_String[j] = "0";
                    } else {
                        HEAD_BUFFER_STATUS_String[j] = "1";
                    }

                }
                HEAD_BUFFER_STATUS_String[3] = "0";

                toArduino = String.join("", HEAD_BUFFER_STATUS_String);

                relay_udp.send("t" + toArduino, relayip, 9876);

                delay(300);
            }

            HEAD_BUFFER_STATUS_String = new String[]{"0", "0", "0", "1",  "0", "0", "0", "0"};
            toArduino = String.join("", HEAD_BUFFER_STATUS_String);

            relay_udp.send("t" + toArduino, relayip, 9876);

            delay(1000);

            return;

        }




/*
        //delay(1000);
        if(SETTING_COR!=null){
            InitiateHead(SETTING_COR);
            return;
        }


        if(!initiated){
            ShuffleInitiate(shufflePool);
            initiated = true;
        }


        //如果有名字输入
        if(nameNow!=preName){
            preName = nameNow;
            ANI_MODE = false;
            NameTime = 0;
            println("name mode on");
            int ind = NAMES_ARRAY.indexOf(nameNow);
            nameCorNow = NAMES_COR[ind];
            for (Wave w:shufflePool
                 ) {
                w.Initiate();
            }
            queueWave.Initiate();

        }


        if(ANI_MODE){
            //AniTime++;
            if(AniTime %switchTime == 0){
                if(shuffleMode){//switch to queueMode
                    if(debugMode)println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!change to queue mode!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    for (Wave w:shufflePool) {
                        w.Initiate();
                    }
                    shuffleMode = false;
                    queueMode = true;
                    for (Head h:heads) {
                        h.SetWave(queueWave);
                    }
                    QueueInitiate(queueWave);

                }

                else if(queueMode){//switch to shuffleMode
                    if(debugMode)println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!change to shuffle mode!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    queueWave.QueueInitiate();
                    int n = (int)random(waveNum);
                    queueWave = waves.get(n);
                    shuffleMode = true;
                    queueMode = false;
                    for(int i = 0;i<shufflePool.length;i++){
                        heads[i].SetWave(shufflePool[i]);
                    }
                    ShuffleInitiate(shufflePool);



                }
            }
            if(shuffleMode&&ANI_MODE){
                for(int i = 0;i<shufflePool.length;i++){
                    Wave wNow = shufflePool[i];
                    if(wNow.process<wNow.pts.length&&!heads[i].startBuffer){
                        heads[i].PointAtTarget(port,wNow.pts[wNow.process],servo_udp);
                        wNow.process++;
                        //如果走到尽头，则换wave,且相应的云台转向新wave的起始点，并设置延迟
                        if(wNow.process >= wNow.pts.length){
                            wNow.status = false;
                            wNow.Initiate();
                            int n = (int)random(waveNum);
                            shufflePool[i] = waves.get(n);
                            heads[i].SetWave(waves.get(n));
                            heads[i].PointAtTarget(port,shufflePool[i].pts[0],servo_udp);
                            if(debugMode)println("shuffle change! head:"+heads[i].headID);
                        }
                    }else if(heads[i].startBuffer){
                        heads[i].AddBuffer();
                    }
                }
            }
        }




        else if(queueMode&&ANI_MODE){
            if(queueWave.queueEnd){
                queueWave.QueueInitiate();
                int n = (int)random(waveNum);
                queueWave = waves.get(n);
                for (Head h:heads
                     ) {h.SetWave(queueWave);

                }
            }

            if(!heads[0].startBuffer){
                PVector[] targets = queueWave.GetQueue();
                for(int i = 0;i<targets.length;i++){
                    heads[i].PointAtTarget(port,targets[i],servo_udp);
                }
            }else{
                for (Head h:heads) {
                    h.AddBuffer();
                }
            }


        }

        else if(NAME_MODE){
            NameTime++;
            InitiateHead(nameCorNow);
            if(NameTime >=nameTime){
                NAME_MODE = false;
                ANI_MODE = true;

                AniTime = 0;
                shufflePool = new Wave[HeadNum];
                for(int i = 0;i<HeadNum;i++){
                    int n = (int)random(waveNum);
                    waves.get(n).status = true;
                    shufflePool[i] = waves.get(n);
                    heads[i].SetWave(waves.get(n));
                }
                ShuffleInitiate(shufflePool);
            }

        }


        toArduino = String.join("",HEAD_BUFFER_STATUS_String);
        if(!toArduino.equals(toArduinoPre)){
            toArduinoPre = toArduino;
            //arduinoPort.write(toArduino);
            //relay_udp.send("t"+toArduino,relayip,9876);
            //println(toArduino);
        }


        if(debugMode)println("--------------------------------------");



       */
    }

    private void ShuffleInitiate(Wave[] ws){
        PVector[] ps = new PVector[ws.length];
        for(int i = 0;i<ps.length;i++){
            ps[i] = ws[i].pts[0];
        }
        InitiateHead(ps);
    }

    private void QueueInitiate(Wave w){
        InitiateHead(w.pts[0]);
    }

    private void InitiateHead(PVector p){
        for(int i = 0;i<heads.length;i++){
            heads[i].PointAtTarget(port,p,servo_udp);
        }

        if(debugMode)println("===============================QueueDelay==============================");
        delay(switchDelayTime);
    }

    private void InitiateHead(PVector[] ps){
        for(int i = 0;i<heads.length;i++){
            heads[i].PointAtTarget(port,ps[i],servo_udp);
        }

        if(debugMode)println("===============================ShuffleDelay==============================");
        delay(switchDelayTime);
    }

    public static void SetHeadBufferStatus(int ID,boolean status){
        HEAD_BUFFER_STATUS[ID] = status;
        int i = status?0:1;
        HEAD_BUFFER_STATUS_String[ID] = String.valueOf(i);
    }

    public class Wave{

        boolean status = false;//是否正在被调用
        int process = 0;//进行位置
        int queue = 0;
        int waveID;

        public int[] que;
        boolean queueEnd = false;
        public PVector[] pts;//将自己所有的点储存在数组中
        public Wave(String ptsAdString,int ID){
            String[] points = split(ptsAdString,"|");
            pts = new PVector[points.length];
            for(int i = 0;i<points.length;i++){
                String[] cord = split(points[i],",");
                pts[i] = new PVector(Float.parseFloat(cord[0]),Float.parseFloat(cord[1]));
                //print("waveID:"+ID+"  corx:"+pts[i].x+"  cory:"+pts[i].y);
            }
            que = new int[ServoController.HeadNum];
            Arrays.fill(que, 0);

            waveID = ID;
        }

        public void Initiate(){

            process = 0;
        }

        public void QueueInitiate(){
            Arrays.fill(que, 0);
            queueEnd = false;
        }

        public PVector[] GetQueue(){
            int[] Q = RenewQueue();
            PVector[] ps = new PVector[Q.length];
            for(int i = 0;i<Q.length;i++){
                ps[i] = pts[Q[i]];
            }
            return ps;
        }

        private int[] RenewQueue(){

            for(int i = 0;i<que.length;i++){
                que[i] = que[0]-i;
            }
            int[] Q = new int[que.length];
            for(int i = 0;i<que.length;i++){
                Q[i] = que[i];
                if(Q[i]<0)Q[i] = 0;
                if(Q[i]>=pts.length)Q[i] = pts.length-1;
            }

            boolean end = true;
            for (int q:Q) {
                if(q<pts.length-1)end = false;
            }
            if(end){
                queueEnd = true;
            }
            que[0]++;
            return Q;
        }


    }
    static class NameInput implements Runnable{

        Scanner reader = new Scanner(System.in);

        @Override
        public void run() {
            while(reader.hasNextLine()){
                String name = reader.nextLine();
                ServoController.nameNow = name;
                System.out.println(name+"<==========================");
            }
        }
    }


}
