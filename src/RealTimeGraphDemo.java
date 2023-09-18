import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortDataListener;

public class RealTimeGraphDemo extends JFrame {

    private XYSeries dataSeries;
    private SerialPort arduinoPort; // Serial port for Arduino communication
    private JButton toggleButton;
    private JLabel temperatureDisplay;
    private JLabel dataStatus;
    private boolean sensorOn = true;
    private Timer timer;
    private int xCounter = 0;
    private double[] xValues = new double[1000]; // Adjust the size as needed
    private double[] yValues = new double[1000]; // Adjust the size as needed
    private boolean isDataReceived = false;
    private double previousTemperature = Double.NaN; // Initialize with NaN to handle the first data point

    public RealTimeGraphDemo(final String title) {
        super(title);

        dataSeries = new XYSeries("Temperature Data");
        XYSeriesCollection dataset = new XYSeriesCollection(dataSeries);


        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);

        chartPanel.setSize(400,400);
       // chartPanel.setSize(new Dimension(800, 800));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setZoomTriggerDistance(Integer.MAX_VALUE);

        dataStatus = new JLabel();
        temperatureDisplay = new JLabel();
        dataStatus.setText("Not Receiving values");
        temperatureDisplay.setText("0° Celsius");
        dataStatus.setBounds(750,1000,200,200);
        temperatureDisplay.setBounds(0,800,200,200);
        dataStatus.setVisible(true);
        temperatureDisplay.setVisible(true);

        setLayout(new BorderLayout());
        add(chartPanel, BorderLayout.CENTER);


        toggleButton = new JButton("Toggle Sensor");
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSensor();
            }
        });
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(toggleButton);
        add(buttonPanel, BorderLayout.SOUTH);
        add(dataStatus,BorderLayout.NORTH);
        add(temperatureDisplay,BorderLayout.SOUTH);

        // Initialize the Arduino serial port (change the port name as needed)
        arduinoPort = SerialPort.getCommPort("COM3"); // Change this to your Arduino's port
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
                    dataStatus.setText("Not Receiving Data");
                else{
                    dataStatus.setText("Receiving Data");
                } ;

                byte[] newData = new byte[arduinoPort.bytesAvailable()];
                int numRead = arduinoPort.readBytes(newData, newData.length);
                String receivedData = new String(newData);

                try {
                    // Parse the received data as a double
                    double temperature = Double.parseDouble(receivedData.trim());

                    // Check if the temperature is within a reasonable range and doesn't deviate too much from the previous reading
                    if (isValidTemperature(temperature) && isValidTemperatureDeviation(temperature)) {
                        SwingUtilities.invokeLater(() -> {
                            isDataReceived = true;
                            dataSeries.add(xCounter, temperature);
                            xValues[xCounter] = xCounter;
                            yValues[xCounter] = temperature;
                            temperatureDisplay.setText(String.valueOf(temperature));
                            xCounter++;
                            // Update the previous temperature value
                            previousTemperature = temperature;
                        });
                    } else {
                        // Data is outside the valid temperature range or has a large deviation, ignore it
                        System.err.println("Invalid temperature value received from Arduino: " + temperature);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid data received from Arduino: " + receivedData);
                }
            }
        });

        // Create and start a timer to advance the x-axis
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sensorOn) {
                    if (!isDataReceived) {
                        // If no data received, add a gap by setting the y-value to NaN
                        dataSeries.add(xCounter, Double.NaN);
                    }
                    xCounter++;
                    isDataReceived = false;
                }
            }
        });
        timer.start();
    }

    private JFreeChart createChart(final XYSeriesCollection dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Real-Time Temperature Graph",
                "Time (s)",
                "Temperature (°C)",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false);

        XYPlot plot = (XYPlot) chart.getPlot();
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        plot.setRenderer(renderer);

        return chart;
    }

    private void toggleSensor() {
        sensorOn = !sensorOn;
        if (sensorOn) {
            toggleButton.setText("Turn Off Sensor");

        } else {
            toggleButton.setText("Turn On Sensor");
        }
    }

    // Define a method to validate the temperature data
    private boolean isValidTemperature(double temperature) {
        // You can adjust this range as needed for your specific temperature range
        return (temperature >= -50 && temperature <= 150);
    }

    // Define a method to validate temperature deviation
    private boolean isValidTemperatureDeviation(double temperature) {
        // Define a threshold for temperature deviation (e.g., 20 degrees)
        double temperatureDeviationThreshold = 20.0;

        // If previousTemperature is NaN, accept the first data point
        if (Double.isNaN(previousTemperature)) {
            return true;
        }

        // Check if the deviation is within the threshold
        double deviation = Math.abs(temperature - previousTemperature);
        return deviation <= temperatureDeviationThreshold;
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
