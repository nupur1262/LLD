package ECommerce.Models;

import java.util.List;

public class Order 
{
    private String OrderId;
    private List<OrderItem> OrderItems;
    private String UserId;
    private Double totalAmt;
    private String AddressId;
    private Payment payment;
    private List<Observer> observers;
    private OrderState state;
    
    public Order(String orderId, List<OrderItem> orderItems, String userId, Double totalAmt, String addressId, Payment payment, List<Observer> observers, OrderState state) {
        OrderId = orderId;
        OrderItems = orderItems;
        UserId = userId;
        this.totalAmt = totalAmt;
        AddressId = addressId;
        this.payment = payment;
        this.observers = observers;
        this.state = state;
    }

    public String getOrderId() {
        return OrderId;
    }

    public void setOrderId(String orderId) {
        OrderId = orderId;
    }

    public List<OrderItem> getOrderItems() {
        return OrderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        OrderItems = orderItems;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public Double getTotalAmt() {
        return totalAmt;
    }

    public void setTotalAmt(Double totalAmt) {
        this.totalAmt = totalAmt;
    }

    public String getAddressId() {
        return AddressId;
    }

    public void setAddressId(String addressId) {
        AddressId = addressId;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public List<Observer> getObservers() {
        return observers;
    }

    public void setObservers(List<Observer> observers) {
        this.observers = observers;
    }

    public OrderState getState() {
        return state;
    }

    public void setState(OrderState state) {
        this.state = state;
    }
}
