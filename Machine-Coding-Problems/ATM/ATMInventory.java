package ATM;

import java.util.HashMap;
import java.util.Map;

public class ATMInventory {
    private Map<CashType, Integer> inventory = new HashMap<>();
    private int totalCash=0;
    public void addCash(CashType type, int val)
    {
        inventory.put(type,inventory.getOrDefault(type,0)+val);
        totalCash+= type.getValue()*val;
    }
    public synchronized int getTotalCash()
    {
        return totalCash;
    }
    private Boolean hasSufficientCash(int cash)
    {
        if(totalCash>cash)
            return true;
        return false;
    }
    public synchronized void dispenseCash(int cash)
    {
        if(hasSufficientCash(cash))
        {
            totalCash-=cash;
        }
        else
        {
            throw new IllegalStateException("ATM out of cash");
        }
    }
}
