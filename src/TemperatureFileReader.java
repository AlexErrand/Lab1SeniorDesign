import java.io.*;
import java.util.Objects;

public class TemperatureFileReader {

    private final BufferedReader br;
    private static String lastReadValue = "2";
    private final static String file_path = "C:\\Users\\Brand\\Documents\\GitHub\\Lab1SeniorDesign\\bluetooth_data.txt";

    public TemperatureFileReader() throws IOException {
        File file = new File(file_path);
        FileWriter fw = new FileWriter(file);
        fw.write(""); // overwrite previous data
        fw.close();
        FileReader fr = new FileReader(file);
        br = new BufferedReader(fr);
    }

    public double getRecentTempReading() throws IOException {
        String s;
        lastReadValue = "2";
        while ((s = br.readLine()) != null) {
            lastReadValue = s;
        }
        // if the 253 value is received then the sensor is unplugged
        if (Objects.equals(lastReadValue, "2")) return ErrorCodes.NOT_CONNECTED.code;
        if (Objects.equals(lastReadValue, "253.00")) return ErrorCodes.UNPLUGGED.code;
        if (Objects.equals(lastReadValue, "254.00")) return ErrorCodes.CHECK_SUM_ERROR.code;
        // parse value to make sure its valid
        try {
            return Double.parseDouble(lastReadValue);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }
}
