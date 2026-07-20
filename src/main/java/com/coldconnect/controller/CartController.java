package com.coldconnect.controller;

import com.coldconnect.entity.CartItem;
import com.coldconnect.i18n.AppMessages;
import com.coldconnect.repository.UserRepository;
import com.coldconnect.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/cart")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Cart", description = "Shopping cart for marketplace lots")
public class CartController extends BaseController {

    private final CartService cartService;
    private final AppMessages messages;

    public CartController(UserRepository userRepository,
                          CartService cartService,
                          AppMessages messages) {
        super(userRepository);
        this.cartService = cartService;
        this.messages    = messages;
    }

    public record AddItemRequest(
            @NotBlank String lotId,
            @NotNull @Positive(message = "Quantity must be greater than zero") Double kg
    ) {}

    public record UpdateItemRequest(
            @NotNull @Positive(message = "Quantity must be greater than zero") Double kg
    ) {}

    @Operation(summary = "Get cart with totals")
    @GetMapping
    public ResponseEntity<CartService.CartSummary> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUser(userDetails).getId();
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @Operation(summary = "Add lot to cart")
    @PostMapping("/items")
    public ResponseEntity<Map<String, Object>> addItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddItemRequest req) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        CartItem item = cartService.addItem(userId, req.lotId(), req.kg(), lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.CART_ITEM_ADDED, lang),
                "item",    item
        ));
    }

    @Operation(summary = "Update cart item quantity")
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<Map<String, Object>> updateItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateItemRequest req) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        CartItem item = cartService.updateItem(userId, itemId, req.kg(), lang);
        return ResponseEntity.ok(Map.of(
                "message", messages.get(AppMessages.Key.CART_ITEM_UPDATED, lang),
                "item",    item
        ));
    }

    @Operation(summary = "Remove item from cart")
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Map<String, String>> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long itemId) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(Map.of("message",
                messages.get(AppMessages.Key.CART_ITEM_REMOVED, lang)));
    }

    @Operation(summary = "Clear entire cart")
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        String lang   = resolveLanguage(userDetails);
        Long   userId = resolveUser(userDetails).getId();
        cartService.clearCart(userId);
        return ResponseEntity.ok(Map.of("message",
                messages.get(AppMessages.Key.CART_CLEARED, lang)));
    }
}