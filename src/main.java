import com.fazecast.jSerialComm.SerialPort;
import java.util.Timer;
import java.io.IOException;
import jssc.SerialPort.*;

public class main {
    public static void main(String[] args) throws IOException, InterruptedException {
        long timeStart = System.currentTimeMillis();

        var sp = SerialPort.getCommPort("COM3");
        sp.setComPortParameters(115200,Byte.SIZE,SerialPort.ONE_STOP_BIT,SerialPort.NO_PARITY);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING,0,0);

        var hasOpened = sp.openPort();
        if(!hasOpened){
            throw new IllegalStateException("Failed to open serial port");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {sp.closePort();}));

        var timer = new Timer();
        var timedSchedule = new TimerScheduleHandler(timeStart);
        sp.addDataListener(timedSchedule);


        System.out.println("Listen" + timedSchedule.getListeningEvents());
        timer.schedule(timedSchedule,0,1000);


    }

}


