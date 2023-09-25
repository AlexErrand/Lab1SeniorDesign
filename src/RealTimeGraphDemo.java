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
import java.io.IOException;

public class RealTimeGraphDemo extends JFrame {

    private final int maxDataPoints = 300;
    private final XYSeries dataSeries;
    private final XYSeries dataSeriesF;
    private final JButton toggleButton;
    private final JButton toggleTemp;
    private final JLabel temperatureDisplay;
    private final JLabel dataStatus;
    private final JFreeChart chart;
    private final JFreeChart chartF;
    private int xCounter = 0;
    private final static String tempLabelC = " °C";
    private final static String tempLabelF = " °F";
    private final TemperatureFileReader tempReader;


    private boolean axisChangedFlag = false; // Flag to prevent reentry

    private boolean isTemperatureInFahrenheit = false; // Flag to track temperature scale

    public RealTimeGraphDemo(final String title) throws IOException {
        super(title);
        // create temp reader object
        tempReader = new TemperatureFileReader();
        // where temp data will be stored for both C and F
        dataSeries = new XYSeries("Temperature Data");
        dataSeriesF = new XYSeries("Temp Data F");
        XYSeriesCollection dataset = new XYSeriesCollection(dataSeries);
        XYSeriesCollection datasetF = new XYSeriesCollection(dataSeriesF);
        // create chart C
        chart = createChart(dataset, "Temperature (°C)");
        ChartPanel chartPanel = new ChartPanel(chart, true, true, true, false, true);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setZoomTriggerDistance(20);
        chartPanel.setSize(400, 400);
        chartPanel.setBounds(10, 10, 400, 400);
        // create chart F
        chartF = createChart(datasetF, "Temperature (°F)");
        ChartPanel chartPanelF = new ChartPanel(chartF, true, true, true, false , true);
        chartPanelF.setMouseWheelEnabled(true);
        chartPanelF.setZoomTriggerDistance(20);
        chartPanelF.setBounds(10,10,400,400);
        chartPanelF.setVisible(false);
        // null layout to set strict bounds
        setLayout(null);
        // status label
        dataStatus = new JLabel();
        dataStatus.setText("Status: Not Receiving Values");
        dataStatus.setBounds(500, 10, 400, 100);
        dataStatus.setFont(new Font("serif", Font.PLAIN, 24));
        dataStatus.setForeground(Color.RED);
        // temp display label
        temperatureDisplay = new JLabel();
        temperatureDisplay.setText("Temperature: 0° Celsius");
        temperatureDisplay.setBounds(500, 100, 400, 100);
        temperatureDisplay.setFont(new Font("serif", Font.PLAIN, 24));
        temperatureDisplay.setForeground(Color.BLUE);
        // button to toggle between C and F
        toggleTemp = new JButton("Change Temperature to Fahrenheit");
        toggleTemp.setBounds(430, 300, 400, 30);
        toggleTemp.addActionListener(e -> {
            if (isTemperatureInFahrenheit) {
                isTemperatureInFahrenheit = false;
                toggleTemp.setText("Change Temperature to Fahrenheit");
                chartPanelF.setVisible(false);
                chartPanel.setVisible(true);
            } else {
                isTemperatureInFahrenheit = true;
                toggleTemp.setText("Change Temperature to Celsius");
                chartPanelF.setVisible(true);
                chartPanel.setVisible(false);
            }
        });
        // button to toggle the display on the arduino
        toggleButton = new JButton("Toggle Sensor");
        toggleButton.setBounds(750, 420, 200, 30);
        toggleButton.addActionListener(e -> {

        });
        // add items to the layout
        add(chartPanelF);
        add(chartPanel);
        add(dataStatus);
        add(temperatureDisplay);
        add(toggleTemp);
        add(toggleButton);
        // start thread for graph updating once every second
        MultithreadingGraph graph = new MultithreadingGraph();
        graph.start();
    }

    private JFreeChart createChart(final XYSeriesCollection dataset, String yLabel) {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Real-Time Temperature Graph",
                "Time (s)",
                yLabel,
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

    private void addValueToGraph() throws IOException {
        // grab most recent temperature reading
        double temperature = tempReader.getRecentTempReading();
        double temperatureInF = convertToFahrenheit(temperature);

        SwingUtilities.invokeLater(() -> {
            // add the temperature counter for both data series
            if (temperature == -1.0) {
                dataStatus.setText("Sensor is unplugged");
                dataSeries.add(xCounter, null);
                dataSeriesF.add(xCounter, null);
                updateTempDisplay(-1, -1);
            } else {
                dataStatus.setText("Sensor connected");
                dataSeries.add(xCounter, temperature);
                dataSeriesF.add(xCounter, temperatureInF);
                updateTempDisplay(temperature, temperatureInF);
            }
            // remove "first" value of series if we exceed 300 seconds
            if (xCounter >= maxDataPoints) {
                dataSeries.remove(0);
                dataSeriesF.remove(0);
            }
            // increment time, in seconds, counter
            xCounter++;

            // Ensure that the x-axis range stays within bounds
            if (xCounter > maxDataPoints) {
                chart.getXYPlot().getDomainAxis().setRange(xCounter - maxDataPoints, xCounter);
                chartF.getXYPlot().getDomainAxis().setRange(xCounter - maxDataPoints, xCounter);
            }
            // Ensure that the y-axis range stays within bounds
            if (!axisChangedFlag) {
                axisChangedFlag = true;
                double maxY = dataSeries.getMaxY();
                double minY = dataSeries.getMinY();
                chart.getXYPlot().getRangeAxis().setRange(minY - 10, maxY + 10);
                maxY = dataSeriesF.getMaxY();
                minY = dataSeriesF.getMinY();
                chartF.getXYPlot().getRangeAxis().setRange(minY - 10, maxY + 10);
                axisChangedFlag = false;
            }
        });
    }

    private void updateTempDisplay(double tempC, double tempF) {
        if (isTemperatureInFahrenheit) {
            temperatureDisplay.setText(tempF + tempLabelF);
        } else {
            temperatureDisplay.setText(tempC + tempLabelC);
        }
    }


    private boolean isValidTemperature(double temperature) {
        return (temperature >= -50 && temperature <= 150);
    }

    // helper method
    private double convertToFahrenheit(double tempInC) {
        return ((tempInC * 1.8) + 32);
    }

    public static void main(final String[] args) {
        SwingUtilities.invokeLater(() -> {
            RealTimeGraphDemo demo;
            try {
                demo = new RealTimeGraphDemo("Real-Time Graph Demo");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            demo.setSize(1000, 500);
            demo.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            demo.setVisible(true);
        });
    }

    // internal class for graph updating on its own thread
    private class MultithreadingGraph extends Thread {
        public void run() {
            try {
                while (true) {
                    addValueToGraph();
                    Thread.sleep(1000);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
