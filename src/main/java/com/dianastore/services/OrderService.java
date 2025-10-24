package com.dianastore.services;

import com.dianastore.entities.*;
import com.dianastore.repository.OrderRepository;
import com.dianastore.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    public Order createOrderFromCart(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // ✅ Create Order from Cart
        Order order = Order.builder()
                .shopperId(cart.getShopperId())
                .market(cart.getMarket())
                .totalAmount(cart.getTotalAmount())
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .shippingAddress(cart.getShippingAddress())
                .billingAddress(cart.getBillingAddress())// ✅ attach transaction here
                .build();


        // ✅ Copy items
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .subtotal(cartItem.getSubtotal())
                    .order(order)
                    .build();
            order.getItems().add(orderItem);
        }

        // ✅ Save order
        Order savedOrder = orderRepository.save(order);

        // ✅ Update Cart status
        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        return savedOrder;
    }
}
