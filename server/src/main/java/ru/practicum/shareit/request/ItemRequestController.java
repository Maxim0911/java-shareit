package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.util.List;

@RestController
@RequestMapping(path = "/requests")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ItemRequestController {

    private final ItemRequestService itemRequestService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemRequestResponseDto createRequest(@Valid @RequestBody ItemRequestDto itemRequestDto,
                                                @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("POST /requests - create request by user ID: {}", userId);
        return itemRequestService.createRequest(itemRequestDto, userId);
    }

    @GetMapping
    public List<ItemRequestResponseDto> getUserRequests(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("GET /requests - get user requests for user ID: {}", userId);
        return itemRequestService.getUserRequests(userId);
    }

    @GetMapping("/all")
    public List<ItemRequestResponseDto> getAllRequests(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /requests/all - get all requests for user ID: {}, from: {}, size: {}", userId, from, size);
        return itemRequestService.getAllRequests(userId, from, size);
    }

    @GetMapping("/{requestId}")
    public ItemRequestResponseDto getRequestById(
            @PathVariable @Positive Long requestId,
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("GET /requests/{} - get request by ID for user ID: {}", requestId, userId);
        return itemRequestService.getRequestById(requestId, userId);
    }
}