package ru.practicum.shareit.item;

import ru.practicum.shareit.item.dto.*;

import java.util.List;

public interface ItemService {

    ItemDto createItem(ItemCreateDto itemDto, Long userId);

    ItemDto updateItem(Long itemId, ItemUpdateDto itemUpdateDto, Long userId);

    ItemDto getItemById(Long itemId, Long userId);

    List<ItemDto> getAllItemsByOwner(Long userId);

    List<ItemDto> searchItems(String text, int from, int size);

    CommentResponseDto addComment(Long itemId, Long userId, CommentDto commentDto);

    List<ItemDto> getItemsByRequestId(Long requestId);

    List<ItemDto> getItemsByRequestIds(List<Long> requestIds);
}