package ATM;

public class InsertedCardState extends BaseState{
    @Override
    public void authenticatePin(ATM atm, int pin)
    {
        if(atm.getCurrentCard().validatePin(pin))
        {
            System.out.println("Auth success!");
            atm.setCurrentState(new AuthenticatedState());
        }
        else {
            System.out.println("Auth Failed");
            ejectCard(atm);
        }
    }

}
