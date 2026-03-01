package ru.practicum.shareit.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.shareit.request.dto.ItemRequestDto;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@Slf4j
@Validated
public class RequestController {
    private final RequestClient requestClient;

    @PostMapping
    public ResponseEntity<Object> createRequest(@Valid @RequestBody ItemRequestDto requestDto,
                                                @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("POST /requests - create request for user ID: {}", userId);
        return requestClient.createRequest(requestDto, userId);
    }

    @GetMapping
    public ResponseEntity<Object> getUserRequests(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("GET /requests - get user requests for user ID: {}", userId);
        return requestClient.getUserRequests(userId);
    }

    @GetMapping("/all")
    public ResponseEntity<Object> getAllRequests(
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId,
            @RequestParam(defaultValue = "0") @PositiveOrZero int from,
            @RequestParam(defaultValue = "10") @Positive int size) {
        log.info("GET /requests/all - get all requests for user ID: {}, from: {}, size: {}", userId, from, size);
        return requestClient.getAllRequests(userId, from, size);
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<Object> getRequestById(
            @PathVariable @Positive Long requestId,
            @RequestHeader("X-Sharer-User-Id") @Positive Long userId) {
        log.info("GET /requests/{} - get request by ID for user ID: {}", requestId, userId);
        return requestClient.getRequestById(requestId, userId);
    }
}