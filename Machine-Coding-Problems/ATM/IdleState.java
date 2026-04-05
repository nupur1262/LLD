package ATM;

public class IdleState extends BaseState{
    @Override
    public void insertCard(ATM atm, Card card)
    {
        atm.setCurrentCard(card);
        System.out.println("Card inserted. Please authenticate.");
        atm.setCurrentState(new InsertedCardState());
    }

}
