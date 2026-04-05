package ATM;

public class DepositStrategy implements TransactionStrategy{

    @Override
    public void execute(ATM atm, int amount)
    {
        atm.getAccount().deposit(amount);
        atm.getInventory().addCash(CashType.HUNDRED,amount);
    }
}
