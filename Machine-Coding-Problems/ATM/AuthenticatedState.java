package ATM;

public class AuthenticatedState extends BaseState {
    @Override
    public void executeTransaction(ATM atm, TransactionStrategy strategy, int amount)
    {
        strategy.execute(atm,amount);
        atm.setCurrentState(new CompletedState());
    }
}
