package ATM;

public class Account {
    private final String accountNumber;
    private int balance;

    public Account(String accountNumber, int balance) {
        this.accountNumber = accountNumber;
        this.balance = balance;
    }
    public synchronized int getBalance()
    {
        return balance;
    }
    public String getAccountNumber()
    {
        return accountNumber;
    }
    public  synchronized void withdraw(int cash)
    {
        if(balance>cash)
        {
            balance-=cash;
        }
        else {
            throw new IllegalArgumentException("Insufficient Balance");
        }
    }
    public synchronized void deposit(int cash)
    {
        balance+=cash;
    }
}
