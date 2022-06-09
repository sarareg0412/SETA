package statistics;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class StatsResponse {

    @XmlElement(name = "statsList")
    List<Stats> statsList;

    public StatsResponse() {
    }

    public StatsResponse(List<Stats> statsList) {
        this.statsList = statsList;
    }

    public List<Stats> getStatsList() {
        return statsList;
    }

    public void setStatsList(List<Stats> statsList) {
        this.statsList = statsList;
    }

    @Override
    public String toString() {
        StringBuilder str= new StringBuilder();
        List<Stats> stats = statsList;
        if(stats.size() > 0){
            str.append("Taxi ").append(stats.get(0).getTaxiId()).append(" :").append("\n");
            for (Stats stat : statsList){
                str.append(stat.toString()).append("\n");
            }
        }
        return str.toString();
    }
}
