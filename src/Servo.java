import hypermedia.net.UDP;
import processing.core.PApplet;
import processing.serial.*;

public class Servo extends PApplet{
    float oriCor;

    float zOriCor;
    int ID;

    String speed;
    //云台角度修正值
    float correctAngle;
    String[] COMMAND;//<============
    byte[] bCOMMAND;

    public boolean isX;

    public Servo(int ID,String speed,float x,float z){
        this.ID = ID;
        this.speed = speed;
        oriCor = x;
        zOriCor = z;

        //COMMAND
        COMMAND = new String[10];
        bCOMMAND = new byte[10];
        COMMAND[0] = "ff";
        COMMAND[1] = "01";

        //set ID
        String ids1 = Integer.toHexString(ID);
        if(ids1.length() == 1)
            COMMAND[2] = "0"+ids1;
        else COMMAND[2] = ids1;


        //set speed
        COMMAND[3] = speed;
        COMMAND[4] = "00";
        COMMAND[5] = "ff";
        COMMAND[6] = "02";
        COMMAND[7] = COMMAND[2];


        for(int i = 0;i<COMMAND.length-2;i++){
            bCOMMAND[i] = (byte)Integer.parseInt(COMMAND[i],16);
        }


    }

    public void SetCorrectAngle(float x){
        correctAngle = x;
    }


    public String[] PointAtTarget(Serial port, float cor, UDP udp){
        float angle = GetAngle(cor);
        String hex = GetHex(angle);
        if(hex.length() == 3){
            COMMAND[8] = hex.substring(1,3);
            COMMAND[9] = "0"+hex.substring(0,1);
        }else{
            COMMAND[8] = "0";
            COMMAND[9] = "0";
        }

        bCOMMAND[8] = (byte)Integer.parseInt(COMMAND[8],16);
        bCOMMAND[9] = (byte)Integer.parseInt(COMMAND[9],16);

        //port.write(bCOMMAND);
        udp.send(bCOMMAND,ServoController.servoip,9876);
        //println(bCOMMAND);

        if(ServoController.debugMode) PrintStrCollect(COMMAND);

        return COMMAND;
    }

    private float GetAngle(float cor){
        float angle;

        float xx = cor-oriCor;
        if(isX){
            if(xx<0){
                angle = 90-atan(-xx/zOriCor)/PI*180;
            }else{
                angle = 90+atan(xx/zOriCor)/PI*180;
            }
            angle+=correctAngle;
        }
        else{
            angle = atan(-xx/ zOriCor)/PI*180;
            angle+=correctAngle;
        }

        println("angle:"+angle);
        return angle;
    }

    private void PrintStrCollect(String[] s){
        print(ID+":");
        for(int i = 0;i<s.length;i++){
            print(s[i]+" ");
        }

        println();
    }
    private String GetHex(float angle){
        if(angle>150)angle = 150;
        int n = Math.round(2000*(angle/150.0f)+500);
        return Integer.toHexString(n);
    }


}
