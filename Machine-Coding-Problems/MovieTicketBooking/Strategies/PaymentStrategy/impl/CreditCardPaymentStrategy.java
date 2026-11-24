package MovieTicketBooking.Strategies.PaymentStrategy.impl;

import MovieTicketBooking.Strategies.PaymentStrategy.PaymentStrategy;

public class CreditCardPaymentStrategy implements PaymentStrategy {
    @Override
    public void pay()
    {
        System.out.println("Payment done using Credit Card");
    }

    @Override
    public void cancel()
    {
        System.out.println("Payment cancelled Credit Card");
    }
}
