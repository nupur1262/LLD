package ATM;

public class Main {
    public static void main(String[] args) {
        {
            ATM atm = new ATM();

            atm.setAccount(new Account("ABC123", 5000));
            atm.getInventory().addCash(CashType.FIVE_HUNDRED, 10);

            atm.setCurrentState(new IdleState());

            atm.getCurrentState().insertCard(atm, new Card("123", 1234));

            atm.getCurrentState().authenticatePin(atm, 12345);
            atm.getCurrentState().executeTransaction(atm, new WithDrawStrategy(), 500);
        }
    }
}