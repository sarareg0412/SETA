package simulator;

import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class MeasurementsBuffer implements Buffer{

    private ArrayList<Measurement> buffer;
    private List<Measurement> measurementAvgQueue = new ArrayList<>();                // Queue of the measurement's averages
    int id = 0;

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
        ArrayList<Measurement> queueCopy = new ArrayList<>(measurementAvgQueue);
        measurementAvgQueue.clear();
        return queueCopy;
    }

    public List<Measurement> slide(){
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
        List<Measurement> list = slide();
        double avg = list.stream().mapToDouble(Measurement::getValue).average().orElse(0.0);
        measurementAvgQueue.add(new Measurement(String.valueOf(id++), "PM10", avg, System.currentTimeMillis()));
    }
}
