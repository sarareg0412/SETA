package taxi;

import javax.xml.bind.annotation.XmlRootElement;

/* Class that holds the taxi's most important infos: id; port and address*/
@XmlRootElement
public class TaxiInfo {
    private String id;
    private int port;
    private String address;

    public TaxiInfo() {
    }

    public TaxiInfo(String id, int port, String address) {
        this.id = id;
        this.port = port;
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                ", port=" + port +
                ", address='" + address + '\'' +
                '}';
    }
}
