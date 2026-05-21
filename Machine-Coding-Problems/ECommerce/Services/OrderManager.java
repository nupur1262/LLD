package ECommerce.Services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import ECommerce.Models.Cart;
import ECommerce.Models.Order;
import ECommerce.Models.User;
import ECommerce.Strategies.PaymentStrategy;

public class OrderManager {
    // placeOrder(cart)
    // cancelOrder(order)
    // trackOrder(order)
    // updateOrder(order)

    private Map<String, Order> orders = new HashMap<>();

    public Order placeOrder(User user, Cart cart, PaymentStrategy paymentStrategy)
    {
        System.out.println("Order To be Placed");
        return null;
    }
}
