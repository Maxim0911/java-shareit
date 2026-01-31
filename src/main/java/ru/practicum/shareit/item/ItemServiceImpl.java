package ru.practicum.shareit.item;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
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

    @Override
    @Transactional
    public ItemDto createItem(ItemCreateDto itemDto, Long userId) {
        log.info("Creating item for user ID: {}", userId);

        validateItemForCreation(itemDto);

        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        Item item = ItemMapper.toItem(itemDto, owner);
        Item savedItem = itemRepository.save(item);

        log.info("Item created with ID: {}", savedItem.getId());
        return ItemMapper.toItemDto(savedItem);
    }

    @Override
    @Transactional
    public ItemDto updateItem(Long itemId, ItemDto itemDto, Long userId) {
        log.info("Updating item ID: {} for user ID: {}", itemId, userId);

        Item existingItem = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));

        if (!existingItem.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Only owner can update item");
        }

        boolean updated = false;

        if (itemDto.getName() != null) {
            if (itemDto.getName().isBlank()) {
                throw new ValidationException("Name cannot be blank");
            }
            existingItem.setName(itemDto.getName());
            updated = true;
        }

        if (itemDto.getDescription() != null) {
            if (itemDto.getDescription().isBlank()) {
                throw new ValidationException("Description cannot be blank");
            }
            existingItem.setDescription(itemDto.getDescription());
            updated = true;
        }

        if (itemDto.getAvailable() != null) {
            existingItem.setAvailable(itemDto.getAvailable());
            updated = true;
        }

        if (!updated) {
            return getItemDtoWithBookingsAndComments(existingItem);
        }

        Item updatedItem = itemRepository.save(existingItem);
        return getItemDtoWithBookingsAndComments(updatedItem);
    }

    @Override
    public ItemDto getItemById(Long itemId) {
        log.info("Getting item by ID: {}", itemId);

        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found with id: " + itemId));

        return getItemDtoWithBookingsAndComments(item);
    }

    @Override
    public List<ItemDto> getAllItemsByOwner(Long userId) {
        log.info("Getting all items for owner ID: {}", userId);

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        List<Item> items = itemRepository.findAllByOwnerId(userId);
        LocalDateTime now = LocalDateTime.now();

        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .collect(Collectors.toList());

        List<Comment> allComments = commentRepository.findAllByItemIdInOrderByCreatedDesc(itemIds);

        Map<Long, List<CommentResponseDto>> commentsByItem = allComments.stream()
                .collect(Collectors.groupingBy(
                        comment -> comment.getItem().getId(),
                        Collectors.mapping(CommentMapper::toCommentResponseDto, Collectors.toList())
                ));

        return items.stream()
                .map(item -> {
                    Optional<Booking> lastBooking = itemRepository.findLastBooking(item.getId(), now);
                    Optional<Booking> nextBooking = itemRepository.findNextBooking(item.getId(), now);
                    List<CommentResponseDto> comments = commentsByItem.getOrDefault(item.getId(), Collections.emptyList());

                    return ItemMapper.toItemDto(
                            item,
                            lastBooking.orElse(null),
                            nextBooking.orElse(null),
                            comments
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ItemDto> searchItems(String text) {
        log.info("Searching items with text: {}", text);

        if (text == null || text.isBlank()) {
            return List.of();
        }

        return itemRepository.searchAvailableItems(text).stream()
                .map(ItemMapper::toItemDto) // Для поиска не нужно показывать бронирования и комментарии
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
        Comment savedComment = commentRepository.save(comment);

        log.info("Comment added with ID: {}", savedComment.getId());
        return CommentMapper.toCommentResponseDto(savedComment);
    }

    private ItemDto getItemDtoWithBookingsAndComments(Item item) {
        LocalDateTime now = LocalDateTime.now();

        Optional<Booking> lastBooking = itemRepository.findLastBooking(item.getId(), now);
        Optional<Booking> nextBooking = itemRepository.findNextBooking(item.getId(), now);

        List<Comment> comments = commentRepository.findAllByItemIdOrderByCreatedDesc(item.getId());
        List<CommentResponseDto> commentDtos = comments.stream()
                .map(CommentMapper::toCommentResponseDto)
                .collect(Collectors.toList());

        return ItemMapper.toItemDto(
                item,
                lastBooking.orElse(null),
                nextBooking.orElse(null),
                commentDtos
        );
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
    }
}