package com.dianastore.controllers;
import com.dianastore.entities.*;
import com.dianastore.repository.AddressRepository;
import com.dianastore.repository.OrderRepository;
import com.dianastore.repository.PaymentTransactionRepository;
import com.dianastore.services.CartService;
import com.dianastore.services.OrderService;
import com.dianastore.services.PayPalOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/{market}/carts")
public class CartController {

    @Autowired
    private CartService cartService;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PayPalOrderService payPalOrderService;

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;
    @Autowired
    private OrderService orderService;
    @GetMapping
    public List<Cart> getAllCarts() {
        return cartService.cartRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cart> getCartById(@PathVariable Long id) {
        return cartService.getCart(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Cart createCart(@RequestBody Cart cart) {
        return cartService.saveCart(cart);
    }

    // PUT endpoint using Cart itself
    @PutMapping("/{id}")
    public Cart updateCart(@PathVariable Long id, @RequestBody Cart updatedCart) {
        return cartService.updateCart(id, updatedCart);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCart(@PathVariable Long id) {
        cartService.deleteCart(id);
        return ResponseEntity.ok("Cart deleted successfully");
    }
    @PostMapping("/{id}/checkout")
    public ResponseEntity<Cart> checkoutCart(@PathVariable Long id) {
        return cartService.getCart(id)
                .map(cart -> {
                    cart.setStatus(CartStatus.CHECKED_OUT);
                    Cart updated = cartService.saveCart(cart);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{cartId}/address")
    public ResponseEntity<Cart> addOrUpdateAddress(
            @PathVariable Long cartId,
            @RequestBody Address address) {

        Cart cart = cartService.getCart(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        Address targetAddress;

        // Determine if we're updating existing address
        if ("SHIPPING".equalsIgnoreCase(address.getType())) {
            Address existing = cart.getShippingAddress();
            if (existing != null) {
                existing.setStreet(address.getStreet());
                existing.setCity(address.getCity());
                existing.setState(address.getState());
                existing.setCountry(address.getCountry());
                existing.setPostalCode(address.getPostalCode());
                existing.setShopperId(address.getShopperId());
                targetAddress = addressRepository.save(existing);
            } else {
                targetAddress = addressRepository.save(address);
                cart.setShippingAddress(targetAddress);
            }
        } else if ("BILLING".equalsIgnoreCase(address.getType())) {
            Address existing = cart.getBillingAddress();
            if (existing != null) {
                existing.setStreet(address.getStreet());
                existing.setCity(address.getCity());
                existing.setState(address.getState());
                existing.setCountry(address.getCountry());
                existing.setPostalCode(address.getPostalCode());
                existing.setShopperId(address.getShopperId());
                targetAddress = addressRepository.save(existing);
            } else {
                targetAddress = addressRepository.save(address);
                cart.setBillingAddress(targetAddress);
            }
        } else {
            return ResponseEntity.badRequest().build();
        }

        Cart updatedCart = cartService.saveCart(cart);
        return ResponseEntity.ok(updatedCart);
    }
    @PostMapping("/{id}/payment")
    public ResponseEntity<String> makePayment(@PathVariable Long id) {
        Cart cart = cartService.getCart(id)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        // Call PayPal API
        Map<String, Object> response = payPalOrderService.createOrder(String.format("%.2f", cart.getTotalAmount()));


        // Extract approval link
        String approvalLink = null;
        Object linksObj = response.get("links");
        if (linksObj instanceof List) {
            List<Map<String, Object>> links = (List<Map<String, Object>>) linksObj;
            for (Map<String, Object> link : links) {
                if ("approve".equals(link.get("rel"))) {
                    approvalLink = (String) link.get("href");
                    break;
                }
            }
        }

        if (approvalLink == null) {
            return ResponseEntity.badRequest().body("No approval link found in PayPal response");
        }

        return ResponseEntity.ok(approvalLink);
    }
    @PostMapping("/{cartId}/placeorder")
    public ResponseEntity<Order> placeOrder(
            @PathVariable Long cartId,
            @RequestParam String transactionId) {

        Cart cart = cartService.getCart(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Invalid transaction ID: " + transactionId));

        if (!"AUTHORIZED".equalsIgnoreCase(transaction.getStatus())) {
            throw new RuntimeException("Payment not authorized yet. Cannot place order.");
        }

        Order order = orderService.createOrderFromCart(cartId);

        transaction.setInternalOrderId(String.valueOf(order.getId()));
        paymentTransactionRepository.save(transaction);

        order.setPaymentTransaction(transaction);
        orderRepository.save(order);

        cartService.saveCart(cart);

        return ResponseEntity.ok(order);
    }


}
