package MovieTicketBooking.Strategies.PaymentStrategy.impl;

import MovieTicketBooking.Strategies.PaymentStrategy.PaymentStrategy;

public class UPIPaymentStrategy implements PaymentStrategy {
    @Override
    public void pay()
    {
        System.out.println("Payment done using UPI");
    }

    @Override
    public void cancel()
    {
        System.out.println("Payment cancelled using UPI");
    }
}
