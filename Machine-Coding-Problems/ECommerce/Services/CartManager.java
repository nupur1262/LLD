package ECommerce.Services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import ECommerce.Models.Cart;
import ECommerce.Models.CartItem;
import ECommerce.Models.Order;
import ECommerce.Models.Product;
import ECommerce.Models.User;
import ECommerce.Strategies.PaymentStrategy;

public class CartManager 
{
    private OrderManager orderManager;
    private Map<String, Cart> userCarts = new HashMap<>();
    
    public void AddToCart(User user, Product product, Integer qty)
    {
        Cart cart = userCarts.get(user.getUserId());
        if (cart == null) 
        {
            String cartId = UUID.randomUUID().toString();
            cart = new Cart(cartId, user.getUserId(), new ArrayList<>());
            userCarts.put(user.getUserId(), cart);
        }
        String cartItemId = UUID.randomUUID().toString();
        CartItem item = new CartItem(cartItemId, cart.getCartId(), product.getId(), qty);
        cart.setCartItems(item);
    }

    public Order checkout(User user, PaymentStrategy paymentStrategy) throws Exception
    {
        Cart cart = userCarts.get(user.getUserId());
        if(cart == null)
        {
            throw new Exception("Cart is empty");
        }
        
        Order order = orderManager.placeOrder(user, cart, paymentStrategy);
        cart.getCartItems().clear();
        return order;
    }
}