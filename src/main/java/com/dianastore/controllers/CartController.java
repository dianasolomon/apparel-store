package com.dianastore.controllers;
import com.dianastore.entities.*;
import com.dianastore.repository.AddressRepository;
import com.dianastore.repository.OrderRepository;
import com.dianastore.repository.PaymentTransactionRepository;
import com.dianastore.services.CartService;
import com.dianastore.services.OrderService;
import com.dianastore.services.PayPalAuthorizationService;
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
    @Autowired
    private PayPalAuthorizationService payPalAuthorizationService;
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
        Map<String, Object> response = payPalOrderService.createOrder(String.format("%.2f", cart.getTotalAmount()),cart);


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
            @PathVariable Long cartId) {
        Order order = orderService.createOrderFromCart(cartId);
        return ResponseEntity.ok(order);
    }
    @GetMapping("/{cartId}/payment/success")
    public ResponseEntity<?> handlePayPalSuccess(
            @PathVariable String market,
            @PathVariable Long cartId,
            @RequestParam("token") String paypalOrderId) {
        try {
            Map<String, Object> result = payPalAuthorizationService.authorizePayment(paypalOrderId);

            PaymentTransaction transaction = paymentTransactionRepository
                    .findByPaypalOrderId(paypalOrderId)
                    .orElseThrow(() -> new RuntimeException("Transaction not found"));

            transaction.setStatus("APPROVED");
            paymentTransactionRepository.save(transaction);

            return ResponseEntity.ok(Map.of(
                    "message", "Payment successful",
                    "cartId", cartId,
                    "paypalOrderId", paypalOrderId,
                    "transactionStatus", transaction.getStatus()
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Payment authorization failed", "details", e.getMessage())
            );
        }
    }

    @GetMapping("/{cartId}/payment/cancel")
    public ResponseEntity<?> onPaymentCancel(
            @PathVariable String market,
            @PathVariable Long cartId) {

        return ResponseEntity.ok(Map.of(
                "message", "Payment was cancelled by the user",
                "cartId", cartId
        ));
    }


}
