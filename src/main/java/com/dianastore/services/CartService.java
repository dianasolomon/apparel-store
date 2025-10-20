package com.dianastore.services;

import com.dianastore.entities.Cart;
import com.dianastore.entities.Product;
import com.dianastore.repository.CartRepository;
import com.dianastore.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CartService {

    @Autowired
    public CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;
    public Optional<Cart> getCart(Long cartId) {
        return cartRepository.findById(cartId);
    }

    public Cart saveCart(Cart cart) {
        if (cart.getItems() != null) {
            cart.getItems().forEach(item -> {
                Product product = productRepository.findById(item.getProduct().getId())
                        .orElseThrow(() -> new RuntimeException("Invalid product ID"));
                item.setProduct(product);
                item.setCart(cart);
            });
        }
        return cartRepository.save(cart);
    }

    // Safe update using Cart itself
    public Cart updateCart(Long cartId, Cart updatedCart) {
        return cartRepository.findById(cartId)
                .map(existingCart -> {
                    if (updatedCart.getShopperId() != null) existingCart.setShopperId(updatedCart.getShopperId());
                    if (updatedCart.getMarket() != null) existingCart.setMarket(updatedCart.getMarket());
                    if (updatedCart.getTotalAmount() != null) existingCart.setTotalAmount(updatedCart.getTotalAmount());
                    if (updatedCart.getStatus() != null) existingCart.setStatus(updatedCart.getStatus());
                    if (updatedCart.getItems() != null) {
                        // Clear existing items
                        existingCart.getItems().clear();

                        // Add new items and set the cart reference
                        updatedCart.getItems().forEach(item -> {
                            item.setCart(existingCart);
                            existingCart.getItems().add(item);
                        });
                    }

                    return cartRepository.save(existingCart);
                })
                .orElseThrow(() -> new RuntimeException("Cart not found with id " + cartId));
    }

    public void deleteCart(Long cartId) {
        cartRepository.deleteById(cartId);
    }
}
