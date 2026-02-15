package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.mapper.CommentMapper;
import ru.practicum.shareit.item.mapper.ItemMapper;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemServiceImpl implements ItemService {

    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final ItemRequestRepository itemRequestRepository;

    @Override
    @Transactional
    public ItemDto createItem(ItemCreateDto itemDto, Long userId) {
        log.info("Creating item for user ID: {}", userId);
        validateItemForCreation(itemDto);

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Item item = ItemMapper.toItem(itemDto, owner);

        if (itemDto.getRequestId() != null) {
            ItemRequest request = itemRequestRepository.findById(itemDto.getRequestId())
                    .orElseThrow(() -> new NotFoundException(
                            "Item request not found with id: " + itemDto.getRequestId()
                    ));
            item.setRequest(request);
        }

        Item savedItem = itemRepository.save(item);
        log.info("Item created with ID: {}", savedItem.getId());
        return getItemDtoWithBookingsAndComments(savedItem, true);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, ItemUpdateDto itemUpdateDto, Long userId) {
        log.info("Updating item ID: {} for user ID: {}", itemId, userId);

        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));

        if (!existingItem.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Only owner can update item");
        }

        boolean updated = false;

        if (itemUpdateDto.getName() != null) {
            if (itemUpdateDto.getName().isBlank()) {
                throw new ValidationException("Name cannot be blank");
            }
            existingItem.setName(itemUpdateDto.getName());
            updated = true;
        }

        if (itemUpdateDto.getDescription() != null) {
            if (itemUpdateDto.getDescription().isBlank()) {
                throw new ValidationException("Description cannot be blank");
            }
            existingItem.setDescription(itemUpdateDto.getDescription());
            updated = true;
        }

        if (itemUpdateDto.getAvailable() != null) {
            existingItem.setAvailable(itemUpdateDto.getAvailable());
            updated = true;
        }

        if (!updated) {
            return getItemDtoWithBookingsAndComments(existingItem, true);
        }

        Item updatedItem = itemRepository.save(existingItem);
        return getItemDtoWithBookingsAndComments(updatedItem, true);
    }

    @Override
    public ItemDto getItemById(Long itemId, Long userId) {
        log.info("Getting item by ID: {} for user ID: {}", itemId, userId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));

        boolean isOwner = item.getOwner().getId().equals(userId);
        return getItemDtoWithBookingsAndComments(item, isOwner);
    }

    @Override
    public List<ItemDto> getAllItemsByOwner(Long userId) {
        log.info("Getting all items for owner ID: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        List<Item> items = itemRepository.findAllByOwnerId(userId);
        return getItemsWithBookingsAndComments(items);
    }

    @Override
    public List<ItemDto> searchItems(String text, int from, int size) {
        log.info("Searching items with text: {}, from: {}, size: {}", text, from, size);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(from / size, size);
        List<Item> items = itemRepository.searchAvailableItems(text.toLowerCase(), pageable);

        // Для поиска не показываем бронирования и комментарии
        return items.stream()
                .map(item -> ItemDto.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .available(item.getAvailable())
                        .ownerId(item.getOwner().getId())
                        .requestId(item.getRequest() != null ? item.getRequest().getId() : null)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentResponseDto addComment(Long itemId, Long userId, CommentDto commentDto) {
        log.info("Adding comment to item ID: {} by user ID: {}", itemId, userId);

        User author = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));

        boolean hasBooked = bookingRepository.findFirstByBookerIdAndItemIdAndStatusAndEndBefore(
                userId, itemId, BookingStatus.APPROVED, LocalDateTime.now()).isPresent();

        if (!hasBooked) {
            throw new ValidationException("Only users who have booked this item can leave comments");
        }

        if (commentDto.getText() == null || commentDto.getText().isBlank()) {
            throw new ValidationException("Comment text cannot be blank");
        }

        Comment comment = CommentMapper.toComment(commentDto, item, author);
        comment.setCreated(LocalDateTime.now());
        Comment savedComment = commentRepository.save(comment);

        log.info("Comment added with ID: {}", savedComment.getId());
        return CommentMapper.toCommentResponseDto(savedComment);
    }

    @Override
    public List<ItemDto> getItemsByRequestId(Long requestId) {
        log.info("Getting items by request ID: {}", requestId);

        itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Item request not found with id: " + requestId));

        List<Item> items = itemRepository.findAllByRequestId(requestId);

        return items.stream()
                .map(item -> ItemDto.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .available(item.getAvailable())
                        .ownerId(item.getOwner().getId())
                        .requestId(requestId)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> getItemsByRequestIds(List<Long> requestIds) {
        log.info("Getting items by request IDs: {}", requestIds);

        if (requestIds == null || requestIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Item> items = itemRepository.findAllByRequestIdIn(requestIds);

        return items.stream()
                .map(item -> ItemDto.builder()
                        .id(item.getId())
                        .name(item.getName())
                        .description(item.getDescription())
                        .available(item.getAvailable())
                        .ownerId(item.getOwner().getId())
                        .requestId(item.getRequest() != null ? item.getRequest().getId() : null)
                        .build())
                .collect(Collectors.toList());
    }

    private ItemDto getItemDtoWithBookingsAndComments(Item item, boolean isOwner) {
        LocalDateTime now = LocalDateTime.now();
        BookingShortDto lastBooking = null;
        BookingShortDto nextBooking = null;

        if (isOwner) {
            Optional<Booking> lastBookingOpt = bookingRepository.findLastBookingForItem(item.getId(), now);
            Optional<Booking> nextBookingOpt = bookingRepository.findNextBookingForItem(item.getId(), now);

            lastBooking = lastBookingOpt.map(booking -> BookingShortDto.builder()
                    .id(booking.getId())
                    .bookerId(booking.getBooker().getId())
                    .build()).orElse(null);

            nextBooking = nextBookingOpt.map(booking -> BookingShortDto.builder()
                    .id(booking.getId())
                    .bookerId(booking.getBooker().getId())
                    .build()).orElse(null);
        }

        List<Comment> comments = commentRepository.findAllByItemIdOrderByCreatedDesc(item.getId());
        List<CommentResponseDto> commentDtos = comments.stream()
                .map(CommentMapper::toCommentResponseDto)
                .collect(Collectors.toList());

        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .ownerId(item.getOwner().getId())
                .requestId(item.getRequest() != null ? item.getRequest().getId() : null)
                .lastBooking(lastBooking)
                .nextBooking(nextBooking)
                .comments(commentDtos)
                .build();
    }

    private List<ItemDto> getItemsWithBookingsAndComments(List<Item> items) {
        if (items.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .collect(Collectors.toList());

        LocalDateTime now = LocalDateTime.now();
        List<Comment> allComments = commentRepository.findAllByItemIdInOrderByCreatedDesc(itemIds);

        Map<Long, Booking> lastBookings = bookingRepository
                .findLastBookingsForItems(itemIds, now)
                .stream()
                .collect(Collectors.toMap(
                        booking -> booking.getItem().getId(),
                        Function.identity()
                ));

        Map<Long, Booking> nextBookings = bookingRepository
                .findNextBookingsForItems(itemIds, now)
                .stream()
                .collect(Collectors.toMap(
                        booking -> booking.getItem().getId(),
                        Function.identity()
                ));

        Map<Long, List<CommentResponseDto>> commentsByItem = allComments.stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentResponseDto, Collectors.toList())
                ));

        return items.stream()
                .map(item -> {
                    Booking lastBooking = lastBookings.get(item.getId());
                    Booking nextBooking = nextBookings.get(item.getId());

                    List<CommentResponseDto> comments = commentsByItem.getOrDefault(
                            item.getId(),
                            Collections.emptyList()
                    );

                    return ItemDto.builder()
                            .id(item.getId())
                            .name(item.getName())
                            .description(item.getDescription())
                            .available(item.getAvailable())
                            .ownerId(item.getOwner().getId())
                            .requestId(item.getRequest() != null ? item.getRequest().getId() : null)
                            .lastBooking(lastBooking != null ? BookingShortDto.builder()
                                    .id(lastBooking.getId())
                                    .bookerId(lastBooking.getBooker().getId())
                                    .build() : null)
                            .nextBooking(nextBooking != null ? BookingShortDto.builder()
                                    .id(nextBooking.getId())
                                    .bookerId(nextBooking.getBooker().getId())
                                    .build() : null)
                            .comments(comments)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void validateItemForCreation(ItemCreateDto itemDto) {
        if (itemDto.getName() == null || itemDto.getName().isBlank()) {
            throw new ValidationException("Name cannot be blank");
        }

        if (itemDto.getDescription() == null || itemDto.getDescription().isBlank()) {
            throw new ValidationException("Description cannot be blank");
        }

        if (itemDto.getAvailable() == null) {
            throw new ValidationException("Available status cannot be null");
        }

        if (itemDto.getRequestId() != null) {
            
        }
    }

    public LocalDateTime getCurrentTime() {
        return LocalDateTime.now();
    }
}