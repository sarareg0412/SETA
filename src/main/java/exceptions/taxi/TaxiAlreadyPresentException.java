package exceptions.taxi;

public class TaxiAlreadyPresentException extends Exception {
    private String message;

    public TaxiAlreadyPresentException(){
        this.message = "Taxi already present in the network";
    }

    @Override
    public String getMessage() {
        return message;
    }
}
