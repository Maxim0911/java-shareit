package ru.practicum.shareit.request;

import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.util.List;

public interface ItemRequestService {
    ItemRequestResponseDto createRequest(ItemRequestDto itemRequestDto, Long userId);

    List<ItemRequestResponseDto> getUserRequests(Long userId);

    List<ItemRequestResponseDto> getAllRequests(Long userId, int from, int size);

    ItemRequestResponseDto getRequestById(Long requestId, Long userId);
}