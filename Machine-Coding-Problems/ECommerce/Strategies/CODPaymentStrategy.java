package ECommerce.Strategies;

public class CODPaymentStrategy implements PaymentStrategy
{
    @Override
    public void pay(Double amount)
    {
        System.out.println("Amount " + amount + " paid using COD");
    }
}
