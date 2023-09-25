import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

public class HC06 {
    // Serial port settings
    private static final String serialPortName = "COM4"; // Replace with your COM port
    private static final int serialBaudRate = 9600;

    // Bluetooth settings
    private static final String hc06Url = "btspp://002111019AAB:1;authenticate=false;encrypt=false;master=false";

    public static void main(String[] args) {
        try {
            new HC06().go();
        } catch (Exception ex) {
            Logger.getLogger(HC06.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void go() throws Exception {
        System.out.println("Setup 1");
        // Initialize the serial port
        SerialPort serialPort = SerialPort.getCommPort(serialPortName);
        serialPort.openPort();
        serialPort.setComPortParameters(serialBaudRate, 8, 1, 0);

        // Initialize the Bluetooth connection
        StreamConnection streamConnection = (StreamConnection) Connector.open(hc06Url);
        BufferedReader reader = new BufferedReader(new InputStreamReader(streamConnection.openInputStream()));

        System.out.println("Setup 2");

        // Add a data listener for the serial port
        serialPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    byte[] newData = new byte[serialPort.bytesAvailable()];
                    int numRead = serialPort.readBytes(newData, newData.length);
                    String receivedData = new String(newData);
                    System.out.println("Serial Data Received: " + receivedData);
                }
            }
        });
        System.out.println("Setup 3");
        // Continuously read and print Bluetooth data
        String bluetoothLine;
        while ((bluetoothLine = reader.readLine()) != null) {
            System.out.println("Bluetooth Data Received: " + bluetoothLine);
        }

        System.out.println("Setup 4");
        // Close the streams and connections (this won't be reached in this example)
        // reader.close();
        // streamConnection.close();
        // serialPort.closePort();
    }
}
