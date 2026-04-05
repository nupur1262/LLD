package ATM;

import java.util.List;

public class ATM
{
    private ATMState currentState;
    private ATMInventory inventory = new ATMInventory(); ;
    private Card currentCard;
    private Account account;
    private List<TransactionStrategy> strategies;

    public ATMInventory getInventory()
    {
        return inventory;
    }
    public Account getAccount()
    {
        return account;
    }
    public Card getCurrentCard(){
        return currentCard;
    }
    public List<TransactionStrategy> getStrategies(){
        return strategies;
    }
    public void setStrategies(List<TransactionStrategy>s)
    {
        strategies=s;
    }
    public ATMState getCurrentState()
    {
        return currentState;
    }
    public void setCurrentState(ATMState state)
    {
        currentState=state;
    }
    public void setCurrentCard(Card card)
    {
        currentCard = card;
    }
    public void setAccount(Account account)
    {
        this.account = account;
    }
}