package ATM;

public class Card {
    private final String cardNumber;
    private final int pin;

    public Card(String cardNumber, int pin) {
        this.cardNumber = cardNumber;
        this.pin = pin;
    }

    int getPin()
    {
        return pin;
    }
    String getCardNumber()
    {
        return cardNumber;
    }
    public boolean validatePin(int pin) {
        return this.pin == pin;
    }
}
