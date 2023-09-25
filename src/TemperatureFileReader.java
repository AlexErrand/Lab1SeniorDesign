import java.io.*;
import java.util.Objects;

public class TemperatureFileReader {

    private final BufferedReader br;
    private static String lastReadValue = "";

    public TemperatureFileReader() throws IOException {
        String file_path = "C:\\Users\\Brand\\Documents\\GitHub\\Lab1SeniorDesign\\bluetooth_data.txt";
        File file = new File(file_path);
        FileWriter fw = new FileWriter(file);
        fw.write(""); // overwrite previous data
        FileReader fr = new FileReader(file);
        br = new BufferedReader(fr);
    }

    public double getRecentTempReading() throws IOException {
        String s;
        while ((s = br.readLine()) != null) {
            lastReadValue = s;
        }
        System.out.println(lastReadValue);
        // if the 253 value is received then the sensor is unplugged
        if (Objects.equals(lastReadValue, "253.00")) return -1.0;
        // parse value to make sure its valid
        try {
            return Double.parseDouble(lastReadValue);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }
}