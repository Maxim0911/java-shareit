package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;


@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ItemController {
    private final ItemClient itemClient;

    @PostMapping
    public ResponseEntity<Object> createItem(@Valid @RequestBody ItemCreateDto itemDto,
                                             @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("POST /items - create item for user ID: {}", userId);
        return itemClient.createItem(itemDto, userId);
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<Object> updateItem(@PathVariable @Positive Long itemId,
                                             @Valid @RequestBody ItemUpdateDto itemUpdateDto,
                                             @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("PATCH /items/{} - update item by user ID: {}", itemId, userId);
        return itemClient.updateItem(itemId, itemUpdateDto, userId);
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<Object> getItemById(@PathVariable @Positive Long itemId,
                                              @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("GET /items/{} - get item by ID, requested by user ID: {}", itemId, userId);
        return itemClient.getItemById(itemId, userId);
    }

    @GetMapping
    public ResponseEntity<Object> getAllItemsByOwner(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /items - get all items for owner ID: {}", userId);
        return itemClient.getAllItemsByOwner(userId, from, size);
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchItems(
            @RequestParam String text,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /items/search - search items with text: '{}'", text);
        if (text.isBlank()) {
            return ResponseEntity.ok().body("[]");
        }
        return itemClient.searchItems(text, from, size);
    }

    @PostMapping("/{itemId}/comment")
    public ResponseEntity<Object> addComment(@PathVariable @Positive Long itemId,
                                             @Valid @RequestBody CommentDto commentDto,
                                             @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("POST /items/{}/comment - add comment by user ID: {}", itemId, userId);
        return itemClient.addComment(itemId, commentDto, userId);
    }
}