package ATM;

public class WithDrawStrategy implements TransactionStrategy{

    @Override
    public void execute(ATM atm, int amount)
    {
        if(atm.getInventory().getTotalCash() > amount && atm.getAccount().getBalance() > amount)
        {
            atm.getAccount().withdraw(amount);
            atm.getInventory().dispenseCash(amount);

            System.out.println("Transaction Successful : Amount : "+amount);
        }
        else if(atm.getInventory().getTotalCash() < amount){
            throw new IllegalArgumentException("Not enough Cash in ATM");
        }
        else {
            throw new IllegalArgumentException("Not enough Balance in your Account");
        }
    }
}
