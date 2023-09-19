import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicLong;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortDataListener;
import javax.swing.*;

public class RealTimeGraphDemo extends JFrame {

    private final XYSeries dataSeries;
    private final SerialPort arduinoPort;
    private final JButton toggleButton;
    private final JLabel temperatureDisplay;
    private final JLabel dataStatus;
    private boolean sensorOn = true;
    private int xCounter = 0;
    private final int maxDataPoints = 300;
    private boolean isDataReceived = false;
    private double previousTemperature = Double.NaN;
    private final long lastUpdateTimestamp = 0;

    private boolean axisChangedFlag = false; // Flag to prevent reentry

    public RealTimeGraphDemo(final String title) {
        super(title);

        dataSeries = new XYSeries("Temperature Data");
        XYSeriesCollection dataset = new XYSeriesCollection(dataSeries);

        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart, true, true, true, true, true);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setZoomTriggerDistance(20);

        chartPanel.setSize(400, 400);

        dataStatus = new JLabel();
        temperatureDisplay = new JLabel();
        dataStatus.setText("Not Receiving values");
        temperatureDisplay.setText("0° Celsius");

        setLayout(null);

        chartPanel.setBounds(10, 10, 400, 400);
        dataStatus.setBounds(750, 10, 200, 20);
        temperatureDisplay.setBounds(10, 420, 200, 20);

        add(chartPanel);
        add(dataStatus);
        add(temperatureDisplay);

        toggleButton = new JButton("Toggle Sensor");
        toggleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleSensor();
            }
        });
        toggleButton.setBounds(750, 420, 200, 30);
        add(toggleButton);

        arduinoPort = SerialPort.getCommPort("COM3");
        arduinoPort.openPort();
        arduinoPort.setBaudRate(115200);

        AtomicLong lastUpdateTimestamp = new AtomicLong();

        arduinoPort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE || !sensorOn)
                    dataStatus.setText("Not Receiving Data");
                else {
                    dataStatus.setText("Receiving Data");
                }

                byte[] newData = new byte[arduinoPort.bytesAvailable()];
                int numRead = arduinoPort.readBytes(newData, newData.length);
                String receivedData = new String(newData);

                try {
                    double temperature = Double.parseDouble(receivedData.trim());

                    if (isValidTemperature(temperature) && isValidTemperatureDeviation(temperature)) {
                        long currentTimestamp = System.currentTimeMillis();

                        if (currentTimestamp - lastUpdateTimestamp.get() >= 1000) {
                            SwingUtilities.invokeLater(() -> {
                                isDataReceived = true;
                                dataSeries.add(xCounter, temperature);
                                if (xCounter >= maxDataPoints) {
                                    dataSeries.remove(0);
                                }
                                temperatureDisplay.setText(String.valueOf(temperature));
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

        Timer timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sensorOn) {
                    if (!isDataReceived) {
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
        plot.setDomainAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
        plot.setRangeAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);

        plot.getDomainAxis().setRange(0, maxDataPoints);
        plot.getRangeAxis().setRange(0, 100);

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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                RealTimeGraphDemo demo = new RealTimeGraphDemo("Real-Time Graph Demo");
                demo.setSize(1000, 500);
                demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                demo.setVisible(true);
            }
        });
    }
}
