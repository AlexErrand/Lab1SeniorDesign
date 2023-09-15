import java.util.Timer;
import java.util.TimerTask;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;



public class TimerScheduleHandler extends TimerTask implements SerialPortDataListener{
    private final long timeStart;
    public TimerScheduleHandler(long timeStart) {
        this.timeStart = timeStart;
    }



    @Override
    public int getListeningEvents() {

        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] newData = event.getReceivedData();
        //System.out.println("Received data of size: " + newData.length);
        for (int i = 0; i < newData.length; ++i){
            System.out.print((char)newData[i]);
        }

        //System.out.println("\n");
    }


    /**
     * The action to be performed by this timer task.
     */
    @Override
    public void run() {
        System.out.println("Time elapsed" + (System.currentTimeMillis() + this.timeStart) + "Milliseconds");
    }


}
