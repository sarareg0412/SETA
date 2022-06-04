package statistics;

import taxi.TaxiInfo;

import java.sql.Timestamp;
import java.util.ArrayList;

public class Stats {
    private String taxiId;
    private Timestamp timestamp;
    private int battery;

    private int kmDriven;
    //Statistics to be sent to the AdministratorServer class
    private ArrayList<Double> airPollutionLev;
    private int completedRides;

    public String getTaxiId() {
        return taxiId;
    }

    public void setTaxiId(String taxiId) {
        this.taxiId = taxiId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public int getKmDriven() {
        return kmDriven;
    }

    public void setKmDriven(int kmDriven) {
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
