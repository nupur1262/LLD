package ECommerce.Strategies;

public class CardPaymentStrategy implements PaymentStrategy
{
    @Override
    public void pay(Double amount)
    {
        System.out.println("Amount " + amount + " paid using Card");
    }
}
