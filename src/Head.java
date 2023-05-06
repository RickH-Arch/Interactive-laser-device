import hypermedia.net.UDP;
import processing.core.PApplet;
import processing.core.PVector;
import processing.serial.*;

public class Head extends PApplet {
    PVector oriCor;
    Servo[] servos;
    int headID;

    boolean startBuffer = false;
    int buffer = 0;
    ServoController.Wave waveNow;
    public Head(int headID,int ID1,int ID2,String speed,PVector oriCor){
        this.oriCor = oriCor;
        servos = new Servo[2];
        servos[0] = new Servo(ID1,speed,oriCor.x,oriCor.z);
        servos[0].isX = true;
        servos[1] = new Servo(ID2,speed,oriCor.y,oriCor.z);
        this.headID = headID;
    }

    public void SetCorrectAngle(float xcorrect,float ycorrect){
        servos[0].SetCorrectAngle(xcorrect);
        servos[1].SetCorrectAngle(ycorrect);

    }

    public void PointAtTarget(Serial port, PVector p, UDP udp){
        if(ServoController.debugMode){
            println();
            print("Head ID:"+headID+"  Wave ID:"+waveNow.waveID);
            println("   "+"cor:"+p.x+","+p.y);
        }


        if(ServoController.HEAD_ACTIVATION[headID]){
            servos[0].PointAtTarget(port,p.x,udp);
            //delay(2500);
            servos[1].PointAtTarget(port, p.y,udp);
            //delay(2500);


        }


    }

    public void AddBuffer(){
        buffer++;
        if(ServoController.debugMode){
            println("HeadID:"+headID+"  buffer:"+buffer);
        }
        if(buffer>ServoController.shuffleBuffer){
            buffer = 0;
            startBuffer = false;
            ServoController.SetHeadBufferStatus(headID,false);
        }
    }

    public void SetWave(ServoController.Wave w){
        startBuffer = true;
        ServoController.SetHeadBufferStatus(headID,true);
        //buffer = 0;
        waveNow = w;
    }

    public void InitiateWave(ServoController.Wave w){
        waveNow = w;
    }
}
