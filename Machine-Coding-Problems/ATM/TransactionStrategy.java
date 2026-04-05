package ATM;

public interface TransactionStrategy {
    void execute(ATM atm, int amount);
}
