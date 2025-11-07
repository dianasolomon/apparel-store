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
    @PostMapping("/{cartId}/placeorder")
    public ResponseEntity<Order> placeOrder(
            @PathVariable Long cartId) {
        Order order = orderService.createOrderFromCart(cartId);
        return ResponseEntity.ok(order);
    }



}
