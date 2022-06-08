package simulator;

import taxi.TaxiUtils;
import utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MeasurementsBuffer implements Buffer{

    private ArrayList<Measurement> buffer;
    public final Object bufferLock;

    public MeasurementsBuffer() {
        buffer = new ArrayList<>();
        bufferLock = new Object();
    }

    @Override
    public void addMeasurement(Measurement m) {
        synchronized (bufferLock) {
            buffer.add(m);              //New measurement is added

            if (buffer.size() == 8)     //The window is full: averages are computed and the buffer is half emptied.
            {
                computeAvg();
            }
        }
    }

    @Override
    public List<Measurement> readAllAndClean() {
        ArrayList<Measurement> l = new ArrayList<>(buffer);
        synchronized (bufferLock){
            for (int i = 0; i < Utils.SLIDING_WINDOWS_BUFFER_LENGTH * Utils.OVERLAP; i++) {
                buffer.remove(0);       //The first element is removed
            }
        }
        return l;
    }

    public void computeAvg(){
        // The list of measurements is retrieved in a controlled way, then
        // the average of the measurements is computed and added to the Queue
        List<Measurement> list = readAllAndClean();
        double avg = 0.0;
        StringBuilder s = new StringBuilder();
        for (Measurement m : list){
            avg += m.getValue();
            s.append(Utils.DECIMAL_FORMAT.format(m.getValue())).append(";");
        }

        System.out.println("> WINDOW: " + s + " AVG: " + avg/Utils.SLIDING_WINDOWS_BUFFER_LENGTH);
        TaxiUtils.getInstance().addAvgToQueue(avg/Utils.SLIDING_WINDOWS_BUFFER_LENGTH);
    }
}
