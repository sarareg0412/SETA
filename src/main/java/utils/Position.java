package utils;

import unimi.dps.ride.RideOuterClass;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Position {
    private int x;
    private int y;

    public Position() {
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Position(RideOuterClass.Ride.PositionMsg positionMsg){
        this.x = positionMsg.getX();
        this.y = positionMsg.getY();
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Position)) {
            return false;
        }

        Position p = (Position) obj;

        return this.x == p.getX() && this.y == p.getY();
    }

    @Override
    public String toString() {
        return "{" + x +
                ", " + y +
                '}';
    }
}
