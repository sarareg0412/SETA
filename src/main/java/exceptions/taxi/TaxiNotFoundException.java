package exceptions.taxi;

public class TaxiNotFoundException extends Exception {
    private String message;

    public TaxiNotFoundException(){
        this.message = "Taxi not found in the network";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
