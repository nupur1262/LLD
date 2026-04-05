package ATM;

public class CompletedState extends BaseState {
    @Override
    public void ejectCard(ATM atm)
    {
        atm.setCurrentState(new IdleState()); System.out.println("Card ejected.");
    }
}
