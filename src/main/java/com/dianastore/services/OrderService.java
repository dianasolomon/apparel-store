package com.dianastore.services;

import com.dianastore.entities.*;
import com.dianastore.repository.OrderRepository;
import com.dianastore.repository.CartRepository;
import com.dianastore.repository.PaymentTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    public Order createOrderFromCart(Long cartId) {
        // ✅ 1. Find Cart
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // ✅ 2. Check if order already created
        if (cart.getStatus() == CartStatus.ORDER_CREATED) {
            throw new RuntimeException("Order already created for this cart");
        }

        // ✅ 3. Ensure PaymentTransaction exists
        PaymentTransaction txn = cart.getPaymentTransaction();
        if (txn == null) {
            throw new RuntimeException("No payment transaction found for this cart");
        }

        // ✅ 4. Re-fetch from DB (optional, for consistency)
        txn = paymentTransactionRepository.findByPaypalOrderId(txn.getPaypalOrderId())
                .orElseThrow(() -> new RuntimeException("Payment transaction not found in DB"));

        // ✅ 5. Ensure payment succeeded
        if (!"APPROVED".equalsIgnoreCase(txn.getStatus()) &&
                !"CAPTURED".equalsIgnoreCase(txn.getStatus())) {
            throw new RuntimeException("Payment not approved or captured");
        }

        // ✅ 6. Validate amount consistency
        double cartAmount = cart.getTotalAmount() != null ? cart.getTotalAmount() : 0.0;
        double txnAmount = txn.getTotalAmount() != null ? txn.getTotalAmount() : 0.0;
        if (Math.abs(cartAmount - txnAmount) > 0.01) {
            throw new RuntimeException("Payment amount does not match cart total");
        }

        // ✅ 7. Create Order
        Order order = Order.builder()
                .shopperId(cart.getShopperId())
                .market(cart.getMarket())
                .totalAmount(cart.getTotalAmount())
                .status(OrderStatus.PENDING)
                .externalReference(cart.getExternalReference())
                .createdAt(LocalDateTime.now())
                .shippingAddress(cart.getShippingAddress())
                .billingAddress(cart.getBillingAddress())
                .paymentTransaction(txn)
                .build();

        // ✅ 8. Copy items
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

        // ✅ 9. Save Order
        Order savedOrder = orderRepository.save(order);

        // ✅ 10. Update cart status
        cart.setStatus(CartStatus.ORDER_CREATED);
        cartRepository.save(cart);

        return savedOrder;
    }
}
