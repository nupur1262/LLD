package ATM;

// Abstract base to avoid repeating code
public abstract class BaseState implements ATMState {
    public void insertCard(ATM atm, Card card) { System.out.println("Invalid: Already inserted."); }
    public void authenticatePin(ATM atm, int pin) { System.out.println("Invalid: Already authenticated."); }
    public void selectTransaction(ATM atm, TransactionStrategy s) { System.out.println("Invalid state."); }
    public void executeTransaction(ATM atm, TransactionStrategy s, int a) { System.out.println("Invalid state."); }
    public void ejectCard(ATM atm) { atm.setCurrentState(new IdleState()); System.out.println("Card ejected."); }
}