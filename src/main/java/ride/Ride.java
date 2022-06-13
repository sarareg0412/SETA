package ride;

import unimi.dps.ride.RideOuterClass;
import utils.Position;
import utils.Utils;

public class Ride {
    String id;
    Position start;
    Position finish;

    public Ride() {
    }

    public Ride(RideOuterClass.Ride rideMsg){
        this.id = rideMsg.getId();
        this.start = new Position(rideMsg.getStart());
        this.finish = new Position(rideMsg.getFinish());
    }

    public Ride(String id, Position start, Position finish) {
        this.id = id;
        this.start = start;
        this.finish = finish;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Position getStart() {
        return start;
    }

    public void setStart(Position start) {
        this.start = start;
    }

    public Position getFinish() {
        return finish;
    }

    public void setFinish(Position finish) {
        this.finish = finish;
    }

    public double getKmToTravel(){
        return Utils.getDistanceBetweenPositions(start,finish);
    }

}
