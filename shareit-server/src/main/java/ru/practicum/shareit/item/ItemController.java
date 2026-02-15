package ru.practicum.shareit.item;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.item.dto.*;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ItemController {

    private final ItemService itemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemDto createItem(@Valid @RequestBody ItemCreateDto itemDto,
                              @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("POST /items - create item for user ID: {}", userId);
        return itemService.createItem(itemDto, userId);
    }

    @PatchMapping("/{itemId}")
    public ItemDto updateItem(@PathVariable @Positive Long itemId,
                              @Valid @RequestBody ItemUpdateDto itemUpdateDto,
                              @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("PATCH /items/{} - update item by user ID: {}", itemId, userId);
        return itemService.updateItem(itemId, itemUpdateDto, userId);
    }

    @GetMapping("/{itemId}")
    public ItemDto getItemById(@PathVariable @Positive Long itemId,
                               @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("GET /items/{} - get item by ID, requested by user ID: {}", itemId, userId);

        return itemService.getItemById(itemId, userId);
    }

    @GetMapping
    public List<ItemDto> getAllItemsByOwner(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /items - get all items for owner ID: {}, from: {}, size: {}", userId, from, size);

        return itemService.getAllItemsByOwner(userId);

    }

    @GetMapping("/search")
    public List<ItemDto> searchItems(
            @RequestParam String text,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /items/search - search items with text: '{}', from: {}, size: {}", text, from, size);
        return itemService.searchItems(text, from, size);
    }

    @PostMapping("/{itemId}/comment")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponseDto addComment(@PathVariable @Positive Long itemId,
                                         @Valid @RequestBody CommentDto commentDto,
                                         @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("POST /items/{}/comment - add comment by user ID: {}", itemId, userId);
        return itemService.addComment(itemId, userId, commentDto);
    }
}