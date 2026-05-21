package ECommerce.Models;

import ECommerce.Enums.PaymentStatus;
import ECommerce.Strategies.PaymentStrategy;

public class Payment {

    private String PaymentId;
    private String OrderId;
    private String transactionId;
    private Double amount;
    private PaymentStatus paymentStatus;
    private PaymentStrategy paymentStrategy;

    public Payment(String paymentId, String orderId, Double amount, PaymentStatus paymentStatus, PaymentStrategy paymentStrategy) {
        PaymentId = paymentId;
        OrderId = orderId;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.paymentStrategy = paymentStrategy;
    }

    public String getPaymentId() {
        return PaymentId;
    }

    public void setPaymentId(String paymentId) {
        PaymentId = paymentId;
    }

    public String getOrderId() {
        return OrderId;
    }

    public void setOrderId(String orderId) {
        OrderId = orderId;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentStrategy getPaymentStrategy() {
        return paymentStrategy;
    }

    public void setPaymentStrategy(PaymentStrategy paymentStrategy) {
        this.paymentStrategy = paymentStrategy;
    }

    public void makePayment(PaymentStrategy paymentStrategy, Double amount)
    {
        paymentStrategy.pay(amount);        
    }
}
