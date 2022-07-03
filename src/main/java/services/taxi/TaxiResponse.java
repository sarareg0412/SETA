package services.taxi;

import taxi.TaxiInfo;
import utils.Position;
import utils.Utils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;


@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class TaxiResponse {

    @XmlElement( name = "taxiList"  )
    private List<TaxiInfo> taxiInfoList;
    @XmlElement( name = "position"  )
    private Position position;

    public TaxiResponse() {
    }

    public TaxiResponse(List<TaxiInfo> taxiInfoList) {
        this.taxiInfoList = taxiInfoList;
        this.position = Utils.getRandomStartingPosition();      //Set taxi's starting position;
    }

    public List<TaxiInfo> getTaxiInfoList() {
        return taxiInfoList;
    }

    public void setTaxiInfoList(List<TaxiInfo> taxiInfoList) {
        this.taxiInfoList = taxiInfoList;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }
}
