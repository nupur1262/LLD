package ECommerce.Models;

public class OrderItem 
{
    private String OrderItemId;
    private String OrderId;
    private String productId;
    private Integer quantity;
    private Double priceAtPurchase;
    
    public OrderItem(String orderItemId, String orderId, String productId, Integer quantity, Double priceAtPurchase) {
        OrderItemId = orderItemId;
        OrderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.priceAtPurchase = priceAtPurchase;
    }

    public String getOrderItemId() {
        return OrderItemId;
    }

    public void setOrderItemId(String orderItemId) {
        OrderItemId = orderItemId;
    }

    public String getOrderId() {
        return OrderId;
    }

    public void setOrderId(String orderId) {
        OrderId = orderId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPriceAtPurchase() {
        return priceAtPurchase;
    }

    public void setPriceAtPurchase(Double priceAtPurchase) {
        this.priceAtPurchase = priceAtPurchase;
    }
}
