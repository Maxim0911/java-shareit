package ru.practicum.shareit.request.mapper;

import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.List;

public class ItemRequestMapper {

    public static ItemRequestResponseDto toItemRequestResponseDto(ItemRequest itemRequest, List<ItemDto> items) {
        if (itemRequest == null) {
            return null;
        }

        return ItemRequestResponseDto.builder()
                .id(itemRequest.getId())
                .description(itemRequest.getDescription())
                .requestorId(itemRequest.getRequestor() != null ? itemRequest.getRequestor().getId() : null)
                .created(itemRequest.getCreated())
                .items(items)
                .build();
    }
}