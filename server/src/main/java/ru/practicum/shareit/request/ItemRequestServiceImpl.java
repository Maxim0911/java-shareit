package ru.practicum.shareit.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.mapper.ItemRequestMapper;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemRequestServiceImpl implements ItemRequestService {

    private final ItemRequestRepository itemRequestRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    @Override
    @Transactional
    public ItemRequestResponseDto createRequest(ItemRequestDto itemRequestDto, Long userId) {
        log.info("Creating item request by user ID: {}", userId);

        User requestor = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        if (itemRequestDto.getDescription() == null || itemRequestDto.getDescription().isBlank()) {
            throw new ValidationException("Description cannot be blank");
        }

        ItemRequest itemRequest = new ItemRequest();
        itemRequest.setDescription(itemRequestDto.getDescription());
        itemRequest.setRequestor(requestor);
        itemRequest.setCreated(LocalDateTime.now());

        ItemRequest savedRequest = itemRequestRepository.save(itemRequest);
        log.info("Item request created with ID: {}", savedRequest.getId());

        return ItemRequestMapper.toItemRequestResponseDto(savedRequest, Collections.emptyList());
    }

    @Override
    public List<ItemRequestResponseDto> getUserRequests(Long userId) {
        log.info("Getting requests for user ID: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(userId);

        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .collect(Collectors.toList());

        List<Item> items = itemRepository.findAllByRequestIdIn(requestIds);

        Map<Long, List<ItemDto>> itemsByRequestId = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getRequest().getId(),
                        Collectors.mapping(ItemMapper::toItemDto, Collectors.toList())
                ));

        return requests.stream()
                .map(request -> {
                    List<ItemDto> requestItems = itemsByRequestId.getOrDefault(request.getId(), Collections.emptyList());
                    return ItemRequestMapper.toItemRequestResponseDto(request, requestItems);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemRequestResponseDto> getAllRequests(Long userId, int from, int size) {
        log.info("Getting all requests for user ID: {}, from: {}, size: {}", userId, from, size);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "created"));

        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdNot(userId, pageable);

        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .collect(Collectors.toList());

        List<Item> items = itemRepository.findAllByRequestIdIn(requestIds);

        Map<Long, List<ItemDto>> itemsByRequestId = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getRequest().getId(),
                        Collectors.mapping(ItemMapper::toItemDto, Collectors.toList())
                ));

        return requests.stream()
                .map(request -> {
                    List<ItemDto> requestItems = itemsByRequestId.getOrDefault(request.getId(), Collections.emptyList());
                    return ItemRequestMapper.toItemRequestResponseDto(request, requestItems);
                })
                .collect(Collectors.toList());
    }

    @Override
    public ItemRequestResponseDto getRequestById(Long requestId, Long userId) {
        log.info("Getting request ID: {} for user ID: {}", requestId, userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        ItemRequest request = itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Item request not found with id: " + requestId));

        List<Item> items = itemRepository.findAllByRequestId(requestId);
        List<ItemDto> itemDtos = items.stream()
                .map(item -> ItemDto.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .available(item.getAvailable())
                        .ownerId(item.getOwner().getId())
                        .requestId(requestId)
                        .build())
                .collect(Collectors.toList());

        return ItemRequestMapper.toItemRequestResponseDto(request, itemDtos);
    }
}