package ATM;

public class ViewBalanceStrategy implements TransactionStrategy{

    @Override
    public void execute(ATM atm, int amount)
    {
        System.out.println("Total Balance in your Account = "+atm.getAccount().getBalance());
    }
}
