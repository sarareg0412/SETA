package taxi;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Taxi {
    private static TaxiInfo taxiInfo;               // Taxi's info: id; port number and address
    private static int batteryLevel;                // Taxi's battery level
    private static int[] position = new int[2];     // Taxi's current position in Cartesian coordinates


    public Taxi(TaxiInfo taxiInfo) {
        //Taxi initialization
        this.taxiInfo = taxiInfo;
    }

    public static void main(String argv[]){

    }

}
