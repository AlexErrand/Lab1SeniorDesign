import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortDataListener;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class RealTimeGraphDemo extends JFrame {
    private XYSeries dataSeries;
    private SerialPort arduinoPort; // Serial port for Arduino communication
    private JButton toggleButton;
    private boolean sensorOn = true;
    public static final String ACCOUNT_SID = "ACf5200104419f75389ab80c169cd5b6c0";
    public static final String AUTH_TOKEN = "5aa6bbba131cc143ee39a9101c23c2cd";

    public RealTimeGraphDemo(final String title) {
        super(title);
        dataSeries = new XYSeries("Temperature Data");
        XYSeriesCollection dataset = new XYSeriesCollection(dataSeries);
        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);

        chartPanel.setPreferredSize(new Dimension(800, 400));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setZoomTriggerDistance(Integer.MAX_VALUE);

        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);

        toggleButton = new JButton("Toggle Sensor");
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!sensorOn){
                    sensorOn = true;
                    toggleButton.setText("Turn Off Sensor");
                    arduinoPort.writeBytes("ON".getBytes(), "ON".length());
                }
                if(sensorOn){
                    toggleButton.setText("Turn On Sensor");
                    arduinoPort.writeBytes("OFF".getBytes(), "OFF".length());
                }
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(toggleButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Initialize the Arduino serial port (change the port name as needed)
        arduinoPort = SerialPort.getCommPort("COM4"); // Change this to your Arduino's port
        arduinoPort.openPort();
        arduinoPort.setBaudRate(115200); // Set the correct baud rate

        // Add a data listener to receive data from Arduino
        arduinoPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE || !sensorOn)
                    return;
                byte[] newData = new byte[arduinoPort.bytesAvailable()];
                int numRead = arduinoPort.readBytes(newData, newData.length);
                String receivedData = new String(newData);

                try {
                    // Parse the received data as a double and add it to the graph
                    double temperature = Double.parseDouble(receivedData.trim());
                    SwingUtilities.invokeLater(() -> {
                        double x = dataSeries.getItemCount() + 1;
                        dataSeries.add(x, temperature);
                        if (temperature > 80) {
                            String warningMessage = "Warning: There was a reported temperature of " + temperature + " at " + x + " seconds, please evacuate";

                            System.out.println(warningMessage);
                        }
                    });
                } catch (NumberFormatException e) {
                    System.err.println("Invalid data received from Arduino: " + receivedData);
                }
            }
        });
    }

    private JFreeChart createChart(final XYSeriesCollection dataset) {
        return ChartFactory.createXYLineChart(
                "Real-Time Temperature Graph",
                "Time (s)",
                "Temperature (°C)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);
    }

    private void toggleSensor() {
        sensorOn = !sensorOn;
        if (sensorOn) {
            toggleButton.setText("Turn Off Sensor");
        } else {
            toggleButton.setText("Turn On Sensor");
        }
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                RealTimeGraphDemo demo = new RealTimeGraphDemo("Real-Time Graph Demo");
                demo.pack();
                demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                demo.setVisible(true);
            }
        });
    }
}
