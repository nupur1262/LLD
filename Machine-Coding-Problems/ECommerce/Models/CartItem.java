package ECommerce.Models;

public class CartItem 
{
    private String cartItemId;
    private String cartId;
    private String productId;
    private Integer quantity;
    private Double currentPrice;

    public CartItem(String cartItemId, String cartId, String productId, Integer quantity) {
        this.cartItemId = cartItemId;
        this.cartId = cartId;
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getCartItemId() {
        return cartItemId;
    }

    public void setCartItemId(String cartItemId) {
        this.cartItemId = cartItemId;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
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

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }
}
