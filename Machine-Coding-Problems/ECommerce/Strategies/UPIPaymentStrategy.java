package ECommerce.Strategies;

public class UPIPaymentStrategy implements PaymentStrategy
{
    @Override
    public void pay(Double amount)
    {
        System.out.println("Amount " + amount + " paid using UPI");
    }
}
