package exceptions.taxi;

public class TaxiNotFoundException extends Exception {
    private String message = "> Taxi not found in the network";

    public TaxiNotFoundException(){
    }

    public TaxiNotFoundException(String message) {
        this.message += " " + message;
    }


    @Override
    public String getMessage() {
        return message;
    }
}
