package taxi;

import exceptions.taxi.TaxiAlreadyPresentException;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TaxiNetwork {

    @XmlElement( name = "taxiList"  )
    private List<Taxi> taxiList;

    private static TaxiNetwork instance;

    public TaxiNetwork() {
        this.taxiList = new ArrayList<>();
    }

    //Singleton instance that returns the list of taxis in the system
    public synchronized static TaxiNetwork getInstance(){
        if(instance==null)
            instance = new TaxiNetwork();
        return instance;
    }

    public synchronized void addTaxi(Taxi taxi) throws TaxiAlreadyPresentException {
        if (!taxiList.contains(taxi))
            taxiList.add(taxi);
        else
            throw new TaxiAlreadyPresentException();
    }
}
