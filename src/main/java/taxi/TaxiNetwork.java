package taxi;

import exceptions.taxi.TaxiAlreadyPresentException;
import exceptions.taxi.TaxiNotFoundException;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class TaxiNetwork {

    private List<TaxiInfo> taxiInfoList;

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

    public synchronized TaxiResponse addTaxiInfo(TaxiInfo taxi) throws TaxiAlreadyPresentException {
        TaxiResponse taxiResponse = new TaxiResponse();

        if (!idAlreadyPresent(taxi.getId())) {
            taxiResponse.setTaxiInfoList(new ArrayList<>(taxiInfoList));
            taxiResponse.setPosition(Utils.getRandomStartingPosition());
            taxiInfoList.add(taxi);                         //Add taxi to list
        }else
            throw new TaxiAlreadyPresentException();

        return taxiResponse;
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
        if(newList.size() > 0){
            for (TaxiInfo t : newList){
                if (t.getId().equals(id))
                    return true;
            }
        }
        return false;
    }

    private TaxiInfo getTaxiInfoById(String id) throws TaxiNotFoundException {
        List<TaxiInfo> newList = this.taxiInfoList;
        for (TaxiInfo t : newList){
            if (t.getId().equals(id))
                return t;
        }
        throw new TaxiNotFoundException();
    }

    public List<TaxiInfo> getTaxiInfoList() {
        return new ArrayList<>(taxiInfoList);
    }

    public void setTaxiInfoList(List<TaxiInfo> taxiInfoList) {
        this.taxiInfoList = taxiInfoList;
    }
}
