package com.dianastore.controllers;
import com.dianastore.entities.*;
import com.dianastore.services.CartService;
import com.dianastore.services.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/{market}/carts")
public class CartController {

    @Autowired
    private CartService cartService;
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
        if (address.getType() == null || address.getType().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Cart updatedCart = cartService.addOrUpdateAddress(cartId, address);
        return ResponseEntity.ok(updatedCart);

    }
    @PostMapping("/{cartId}/placeorder")
    public ResponseEntity<Order> placeOrder(
            @PathVariable Long cartId) {
        Order order = orderService.createOrderFromCart(cartId);
        return ResponseEntity.ok(order);
    }
}
