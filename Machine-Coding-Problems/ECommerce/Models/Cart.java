package ECommerce.Models;

import java.util.List;

public class Cart{
    private String cartId;
    private String userId;
    private List<CartItem> cartItems;

    public Cart(String cartId, String userId, List<CartItem> cartItems) {
        this.cartId = cartId;
        this.userId = userId;
        this.cartItems = cartItems;
    }

    public String getCartId() {
        return cartId;
    }

    public void setCartId(String cartId) {
        this.cartId = cartId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void setCartItems(CartItem cartItem) {
        this.cartItems.add(cartItem);
    }

    public void addCartItems(CartItem cartItem)
    {
        this.cartItems.add(cartItem);
    }

    public void removeCartItems(CartItem cartItem)
    {
        this.cartItems.remove(cartItem);
    }
}
