package ATM;

public interface ATMState
{
    public void insertCard(ATM atm, Card card);
    public void authenticatePin(ATM atm, int pin);
    public void selectTransaction(ATM atm, TransactionStrategy type);
    public void executeTransaction(ATM atm, TransactionStrategy strategy, int amount);
    public void ejectCard(ATM atm);
}
