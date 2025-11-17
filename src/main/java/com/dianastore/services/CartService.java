package com.dianastore.services;

import com.dianastore.entities.Address;
import com.dianastore.entities.Cart;
import com.dianastore.entities.Product;
import com.dianastore.repository.AddressRepository;
import com.dianastore.repository.CartRepository;
import com.dianastore.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class CartService {

    @Autowired
    public CartRepository cartRepository;

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private AddressRepository addressRepository;

    public Optional<Cart> getCart(Long cartId) {
        return cartRepository.findById(cartId);
    }

    public Cart saveCart(Cart cart) {
        if(cart.getId()==null){
            cart.setExternalReference(generateExternalReference());
        }
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

    private String generateExternalReference() {
        String datePart = LocalDate.now().toString().replace("-", "");
        String randomPart = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "SHOP-" + datePart + "-" + randomPart;
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

    public Cart addOrUpdateAddress(Long cartId, Address address) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        Address targetAddress;
        if ("SHIPPING".equalsIgnoreCase(address.getType())) {
            targetAddress = updateOrCreateAddress(cart.getShippingAddress(), address);
            cart.setShippingAddress(targetAddress);
        } else if ("BILLING".equalsIgnoreCase(address.getType())) {
            targetAddress = updateOrCreateAddress(cart.getBillingAddress(), address);
            cart.setBillingAddress(targetAddress);
        } else {
            throw new IllegalArgumentException("Invalid address type");
        }

        return cartRepository.save(cart);
    }

    private Address updateOrCreateAddress(Address existing, Address newAddress) {
        if (existing != null) {
            existing.setStreet(newAddress.getStreet());
            existing.setCity(newAddress.getCity());
            existing.setState(newAddress.getState());
            existing.setCountry(newAddress.getCountry());
            existing.setPostalCode(newAddress.getPostalCode());
            existing.setShopperId(newAddress.getShopperId());
            return addressRepository.save(existing);
        } else {
            return addressRepository.save(newAddress);
        }
    }

}
