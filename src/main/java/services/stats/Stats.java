package services.stats;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement
/* Class holding the statistics to be sent to the AdministratorServer class */
public class Stats {
    private String taxiId;
    private String timestamp;
    private int battery;

    private double kmDriven;
    private ArrayList<Double> airPollutionLev;
    private int completedRides;

    public Stats() {
    }

    public String getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(String taxiId) {
        this.taxiId = taxiId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public double getKmDriven() {
        return kmDriven;
    }

    public void setKmDriven(double kmDriven) {
        this.kmDriven = kmDriven;
    }

    public ArrayList<Double> getAirPollutionLev() {
        return airPollutionLev;
    }

    public void setAirPollutionLev(ArrayList<Double> airPollutionLev) {
        this.airPollutionLev = airPollutionLev;
    }

    public int getCompletedRides() {
        return completedRides;
    }

    public void setCompletedRides(int completedRides) {
        this.completedRides = completedRides;
    }

    @Override
    public String toString() {
        return timestamp +
                ", battery=" + battery +
                "%, kmDriven=" + kmDriven +
                ", airPollutionLev=" + airPollutionLev +
                ", completedRides=" + completedRides;
    }
}
