import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

public class RealTimeGraphDemo extends JFrame {

    private final XYSeries dataSeries;
    private final XYSeries dataSeriesF;
    private final SerialPort arduinoPort;
    private final JButton toggleButton;
    private final JButton toggleTemp;
    private final JLabel temperatureDisplay;
    private final JLabel dataStatus;
    private boolean sensorOn = true;
    private boolean enableDataStream = false;
    private int xCounter = 0;
    private final int maxDataPoints = 300;
    private boolean isDataReceived = false;
    private double previousTemperature = Double.NaN;
    private String tempLabel = " C";

    private boolean isArduinoConnected = true; // Track Arduino connection state

    private boolean axisChangedFlag = false; // Flag to prevent reentry

    private boolean isTemperatureInFahrenheit = false; // Flag to track temperature scale

    public RealTimeGraphDemo(final String title) {
        super(title);

        dataSeries = new XYSeries("Temperature Data");
        dataSeriesF = new XYSeries("Temp Data F");
        XYSeriesCollection dataset = new XYSeriesCollection(dataSeries);
        XYSeriesCollection datasetF = new XYSeriesCollection(dataSeriesF);

        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart, true, true, true, false, true);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setZoomTriggerDistance(20);

        chartPanel.setSize(400, 400);

        JFreeChart chartF = createChart(datasetF);
        ChartPanel chartPanelF = new ChartPanel(chartF, true, true, true, false , true);
        chartPanelF.setMouseWheelEnabled(true);
        chartPanelF.setZoomTriggerDistance(20);
        chartPanelF.setBounds(10,10,400,400);

        dataStatus = new JLabel();
        temperatureDisplay = new JLabel();
        dataStatus.setText("Status: Not Receiving Values");
        temperatureDisplay.setText("Temperature: 0° Celsius");

        setLayout(null);

        chartPanel.setBounds(10, 10, 400, 400);

        temperatureDisplay.setBounds(500, 100, 400, 100);
        temperatureDisplay.setFont(new Font("serif", Font.PLAIN, 24));
        temperatureDisplay.setForeground(Color.BLUE);

        dataStatus.setBounds(500, 10, 400, 100);
        dataStatus.setFont(new Font("serif", Font.PLAIN, 24));
        dataStatus.setForeground(Color.RED);

        add(chartPanelF);
        add(chartPanel);
        add(dataStatus);
        add(temperatureDisplay);
        toggleTemp = new JButton("Change Temperature to Fahrenheit");
        toggleTemp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (isTemperatureInFahrenheit) {
                    isTemperatureInFahrenheit = false;
                    toggleTemp.setText("Change Temperature to Celsius");
                    chartPanelF.setVisible(false);
                    chartPanel.setVisible(true);
                    tempLabel = " C";
                    //arduinoPort.closePort();
                } else {
                    isTemperatureInFahrenheit = true;
                    toggleTemp.setText("Change Temperature to Fahrenheit");
                    chartPanelF.setVisible(true);
                    chartPanel.setVisible(false);
                    tempLabel  = " F";
                    //arduinoPort.openPort();
                }
            }
        });
        toggleTemp.setBounds(430, 300, 400, 30);
        add(toggleTemp);

        toggleButton = new JButton("Toggle Sensor");
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    toggleSensor();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        toggleButton.setBounds(750, 420, 200, 30);
        add(toggleButton);

        arduinoPort = SerialPort.getCommPort("COM3");

        arduinoPort.openPort();
        arduinoPort.setComPortParameters(115200,Byte.SIZE,SerialPort.ONE_STOP_BIT,SerialPort.NO_PARITY);
        arduinoPort.setComPortTimeouts(SerialPort.TIMEOUT_WRITE_BLOCKING,0,0);
        AtomicLong lastUpdateTimestamp = new AtomicLong();

        final byte TIMER_DURATION = 10;

        arduinoPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                byte[] newData = new byte[arduinoPort.bytesAvailable()];
                int numRead = arduinoPort.readBytes(newData, newData.length);
                String receivedData = new String(newData);

                try {

                    double temperature = Double.parseDouble(receivedData.trim());
                    System.out.println(temperature);

                    if (isValidTemperature(temperature) && isValidTemperatureDeviation(temperature)) {
                        long currentTimestamp = System.currentTimeMillis();

                        if (currentTimestamp - lastUpdateTimestamp.get() >= 1000) {
                            SwingUtilities.invokeLater(() -> {

                                dataSeries.add(xCounter, temperature);
                                dataSeriesF.add(xCounter,(temperature*1.8)+32);

                                if (xCounter >= maxDataPoints) {
                                    dataSeries.remove(0);
                                    dataSeriesF.remove(0);
                                }

                                if (isTemperatureInFahrenheit) {
                                    temperatureDisplay.setText((temperature*1.8 + 32) + tempLabel);
                                } else {
                                    temperatureDisplay.setText(temperature + tempLabel);
                                }

                                xCounter++;
                                previousTemperature = temperature;
                                lastUpdateTimestamp.set(currentTimestamp);

                                // Ensure that the x-axis range stays within bounds
                                if (xCounter > maxDataPoints) {
                                    chart.getXYPlot().getDomainAxis().setRange(xCounter - maxDataPoints, xCounter);
                                }
                                // Ensure that the y-axis range stays within bounds
                                if (!axisChangedFlag) {
                                    axisChangedFlag = true;
                                    double maxY = dataSeries.getMaxY();
                                    double minY = dataSeries.getMinY();
                                    chart.getXYPlot().getRangeAxis().setRange(minY - 10, maxY + 10);
                                    axisChangedFlag = false;
                                }
                            });
                        }
                    } else {
                        System.err.println("Invalid temperature value received from Arduino: " + temperature);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid data received from Arduino: " + receivedData);
                }
            }
        });
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
        plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);

        plot.getDomainAxis().setRange(0, maxDataPoints);
        plot.getRangeAxis().setRange(0, 100);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);
        plot.setRenderer(renderer);

        return chart;
    }

    private void toggleSensor() throws IOException {
        sensorOn = !sensorOn;
        OutputStream outputStream = arduinoPort.getOutputStream();
        if (sensorOn) {
            System.out.println("Turn off");
            toggleButton.setText("Turn Off Sensor");
            outputStream.write( 0 );
        } else {
            System.out.println("Turn on");
            toggleButton.setText("Turn On Sensor");
            outputStream.write( 1 );
        }
    }

    private boolean isValidTemperature(double temperature) {
        return (temperature >= -50 && temperature <= 150);
    }

    private boolean isValidTemperatureDeviation(double temperature) {
        double temperatureDeviationThreshold = 20.0;
        if (Double.isNaN(previousTemperature)) {
            return true;
        }
        double deviation = Math.abs(temperature - previousTemperature);
        return deviation <= temperatureDeviationThreshold;
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            RealTimeGraphDemo demo = new RealTimeGraphDemo("Real-Time Graph Demo");
            demo.setSize(1000, 500);
            demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            demo.setVisible(true);
        });
    }
}
