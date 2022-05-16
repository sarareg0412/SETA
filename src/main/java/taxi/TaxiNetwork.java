package taxi;

import exceptions.taxi.TaxiAlreadyPresentException;
import exceptions.taxi.TaxiNotFoundException;
import utils.Position;
import utils.Utils;

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
    private List<TaxiInfo> taxiInfoList;
    @XmlElement( name = "position"  )
    private Position position;

    private static TaxiNetwork instance;

    public TaxiNetwork() {
        this.taxiInfoList = new ArrayList<>();
    }

    //Singleton instance that returns the list of taxis in the system
    public synchronized static TaxiNetwork getInstance(){
        if(instance==null)
            instance = new TaxiNetwork();
        return instance;
    }

    public synchronized void addTaxiInfo(TaxiInfo taxi) throws TaxiAlreadyPresentException {
        if (!idAlreadyPresent(taxi.getId())) {
            taxiInfoList.add(taxi);                         //Add taxi to list
            position = Utils.getRandomStartingPosition();      //Set taxi's starting position
        }else
            throw new TaxiAlreadyPresentException();
    }

    public synchronized void deleteTaxiInfoById(String id) throws TaxiNotFoundException {
        TaxiInfo taxiInfo = getTaxiInfoById(id);
        if (taxiInfo != null)
            taxiInfoList.remove(taxiInfo);
        else
            throw new TaxiNotFoundException();
    }

    private boolean idAlreadyPresent(String id){
        List<TaxiInfo> newList = this.taxiInfoList;
        for (TaxiInfo t : newList){
            if (t.getId().equals(id))
                return true;
        }
        return false;
    }

    private TaxiInfo getTaxiInfoById(String id) throws TaxiNotFoundException {
        List<TaxiInfo> newList = this.taxiInfoList;
        for (TaxiInfo t : newList){
            if (t.getId().equals(id))
                return t;
        }
        return null;
    }
}
