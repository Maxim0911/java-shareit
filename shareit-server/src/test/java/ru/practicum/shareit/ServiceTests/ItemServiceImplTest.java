package ru.practicum.shareit.ServiceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.ItemServiceImpl;
import ru.practicum.shareit.item.dto.*;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceImplTest {

    @Mock
    private ItemRepository itemRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private ItemRequestRepository itemRequestRepository;

    @InjectMocks
    private ItemServiceImpl itemService;

    @Captor
    private ArgumentCaptor<Item> itemCaptor;

    private User owner;
    private User booker;
    private Item item;
    private ItemCreateDto itemCreateDto;
    private ItemUpdateDto itemUpdateDto;
    private CommentDto commentDto;
    private Booking booking;
    private Comment comment;
    private ItemRequest itemRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        owner = new User();
        owner.setId(1L);
        owner.setName("Owner");
        owner.setEmail("owner@test.com");

        booker = new User();
        booker.setId(2L);
        booker.setName("Booker");
        booker.setEmail("booker@test.com");

        item = new Item();
        item.setId(1L);
        item.setName("Дрель");
        item.setDescription("Электрическая дрель");
        item.setAvailable(true);
        item.setOwner(owner);

        itemCreateDto = new ItemCreateDto();
        itemCreateDto.setName("Дрель");
        itemCreateDto.setDescription("Электрическая дрель");
        itemCreateDto.setAvailable(true);
        itemCreateDto.setRequestId(null);

        itemUpdateDto = new ItemUpdateDto();
        itemUpdateDto.setName("Обновленная дрель");
        itemUpdateDto.setDescription("Обновленное описание");
        itemUpdateDto.setAvailable(false);

        commentDto = new CommentDto();
        commentDto.setText("Отличный инструмент!");

        booking = new Booking();
        booking.setId(1L);
        booking.setItem(item);
        booking.setBooker(booker);
        booking.setStart(now.minusDays(5));
        booking.setEnd(now.minusDays(1));
        booking.setStatus(BookingStatus.APPROVED);

        comment = new Comment();
        comment.setId(1L);
        comment.setText("Отличный инструмент!");
        comment.setItem(item);
        comment.setAuthor(booker);
        comment.setCreated(now);

        itemRequest = new ItemRequest();
        itemRequest.setId(1L);
        itemRequest.setDescription("Нужна дрель");
        itemRequest.setRequestor(booker);
        itemRequest.setCreated(now);
    }

    // ==================== Тесты createItem ====================

    @Test
    void createItem_ShouldSuccess() {
        // given
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(commentRepository.findAllByItemIdOrderByCreatedDesc(anyLong())).thenReturn(List.of());

        // when
        ItemDto result = itemService.createItem(itemCreateDto, owner.getId());

        // then
        assertNotNull(result);
        assertEquals(item.getId(), result.getId());
        assertEquals(item.getName(), result.getName());
        assertEquals(item.getDescription(), result.getDescription());
        assertEquals(item.getAvailable(), result.getAvailable());
        assertEquals(owner.getId(), result.getOwnerId());
        assertNull(result.getRequestId());

        verify(userRepository).findById(owner.getId());
        verify(itemRepository).save(any(Item.class));
        verify(commentRepository).findAllByItemIdOrderByCreatedDesc(item.getId());
    }

    @Test
    void createItem_WithRequestId_ShouldLinkToRequest() {
        // given
        itemCreateDto.setRequestId(1L);
        Item itemWithRequest = new Item();
        itemWithRequest.setId(1L);
        itemWithRequest.setName("Дрель");
        itemWithRequest.setDescription("Электрическая дрель");
        itemWithRequest.setAvailable(true);
        itemWithRequest.setOwner(owner);
        itemWithRequest.setRequest(itemRequest);

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(1L)).thenReturn(Optional.of(itemRequest));
        when(itemRepository.save(any(Item.class))).thenReturn(itemWithRequest);
        when(commentRepository.findAllByItemIdOrderByCreatedDesc(anyLong())).thenReturn(List.of());

        // when
        ItemDto result = itemService.createItem(itemCreateDto, owner.getId());

        // then
        assertNotNull(result);
        assertEquals(1L, result.getRequestId());

        verify(itemRequestRepository).findById(1L);
        verify(itemRepository).save(itemCaptor.capture());
        Item savedItem = itemCaptor.getValue();
        assertEquals(itemRequest, savedItem.getRequest());
    }

    @Test
    void createItem_UserNotFound_ShouldThrowException() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.createItem(itemCreateDto, 999L));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createItem_WithInvalidRequestId_ShouldThrowException() {
        // given
        itemCreateDto.setRequestId(999L);
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(itemRequestRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.createItem(itemCreateDto, owner.getId()));

        assertEquals("Item request not found with id: 999", exception.getMessage());
        verify(itemRequestRepository).findById(999L);
        verify(itemRepository, never()).save(any());
    }

    @Test
    void createItem_WithBlankName_ShouldThrowException() {
        // given
        itemCreateDto.setName("   ");

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> itemService.createItem(itemCreateDto, owner.getId()));

        assertEquals("Name cannot be blank", exception.getMessage());
        verify(userRepository, never()).findById(any());
    }

    @Test
    void createItem_WithNullName_ShouldThrowException() {
        // given
        itemCreateDto.setName(null);

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> itemService.createItem(itemCreateDto, owner.getId()));

        assertEquals("Name cannot be blank", exception.getMessage());
    }

    @Test
    void createItem_WithBlankDescription_ShouldThrowException() {
        // given
        itemCreateDto.setDescription("   ");

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> itemService.createItem(itemCreateDto, owner.getId()));

        assertEquals("Description cannot be blank", exception.getMessage());
    }

    @Test
    void createItem_WithNullAvailable_ShouldThrowException() {
        // given
        itemCreateDto.setAvailable(null);

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> itemService.createItem(itemCreateDto, owner.getId()));

        assertEquals("Available status cannot be null", exception.getMessage());
    }

    // ==================== Тесты updateItem ====================

    @Test
    void updateItem_OnlyOwner_ShouldSuccess() {
        // given
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(commentRepository.findAllByItemIdOrderByCreatedDesc(anyLong())).thenReturn(List.of());

        // when
        ItemDto result = itemService.updateItem(item.getId(), itemUpdateDto, owner.getId());

        // then
        assertNotNull(result);
        verify(itemRepository).save(itemCaptor.capture());
        Item updatedItem = itemCaptor.getValue();

        assertEquals(itemUpdateDto.getName(), updatedItem.getName());
        assertEquals(itemUpdateDto.getDescription(), updatedItem.getDescription());
        assertEquals(itemUpdateDto.getAvailable(), updatedItem.getAvailable());
    }

    @Test
    void updateItem_NotOwner_ShouldThrowException() {
        // given
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.updateItem(item.getId(), itemUpdateDto, booker.getId()));

        assertEquals("Only owner can update item", exception.getMessage());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_ItemNotFound_ShouldThrowException() {
        // given
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.updateItem(999L, itemUpdateDto, owner.getId()));

        assertEquals("Item not found with id: 999", exception.getMessage());
    }

    @Test
    void updateItem_OnlyName_ShouldUpdateOnlyName() {
        // given
        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setName("Новое имя");

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(commentRepository.findAllByItemIdOrderByCreatedDesc(anyLong())).thenReturn(List.of());

        // when
        itemService.updateItem(item.getId(), updateDto, owner.getId());

        // then
        verify(itemRepository).save(itemCaptor.capture());
        Item updatedItem = itemCaptor.getValue();

        assertEquals("Новое имя", updatedItem.getName());
        assertEquals(item.getDescription(), updatedItem.getDescription());
        assertEquals(item.getAvailable(), updatedItem.getAvailable());
    }

    @Test
    void updateItem_OnlyDescription_ShouldUpdateOnlyDescription() {
        // given
        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setDescription("Новое описание");

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(commentRepository.findAllByItemIdOrderByCreatedDesc(anyLong())).thenReturn(List.of());

        // when
        itemService.updateItem(item.getId(), updateDto, owner.getId());

        // then
        verify(itemRepository).save(itemCaptor.capture());
        Item updatedItem = itemCaptor.getValue();

        assertEquals(item.getName(), updatedItem.getName());
        assertEquals("Новое описание", updatedItem.getDescription());
        assertEquals(item.getAvailable(), updatedItem.getAvailable());
    }

    @Test
    void updateItem_OnlyAvailable_ShouldUpdateOnlyAvailable() {
        // given
        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setAvailable(false);

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(itemRepository.save(any(Item.class))).thenReturn(item);
        when(commentRepository.findAllByItemIdOrderByCreatedDesc(anyLong())).thenReturn(List.of());

        // when
        itemService.updateItem(item.getId(), updateDto, owner.getId());

        // then
        verify(itemRepository).save(itemCaptor.capture());
        Item updatedItem = itemCaptor.getValue();

        assertEquals(item.getName(), updatedItem.getName());
        assertEquals(item.getDescription(), updatedItem.getDescription());
        assertEquals(false, updatedItem.getAvailable());
    }

    @Test
    void updateItem_EmptyName_ShouldThrowException() {
        // given
        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setName("   ");

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> itemService.updateItem(item.getId(), updateDto, owner.getId()));

        assertEquals("Name cannot be blank", exception.getMessage());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_EmptyDescription_ShouldThrowException() {
        // given
        ItemUpdateDto updateDto = new ItemUpdateDto();
        updateDto.setDescription("   ");

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> itemService.updateItem(item.getId(), updateDto, owner.getId()));

        assertEquals("Description cannot be blank", exception.getMessage());
        verify(itemRepository, never()).save(any());
    }

    @Test
    void updateItem_NoUpdates_ShouldReturnExisting() {
        // given
        ItemUpdateDto updateDto = new ItemUpdateDto();

        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(commentRepository.findAllByItemIdOrderByCreatedDesc(anyLong())).thenReturn(List.of());

        // when
        ItemDto result = itemService.updateItem(item.getId(), updateDto, owner.getId());

        // then
        assertNotNull(result);
        verify(itemRepository, never()).save(any());
    }

    // ==================== Тесты getItemById ====================

    @Test
    void getItemById_ForOwner_ShouldShowBookings() {
        // given
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(bookingRepository.findLastBookingForItem(item.getId(), now)).thenReturn(Optional.of(booking));
        when(bookingRepository.findNextBookingForItem(item.getId(), now)).thenReturn(Optional.empty());
        when(commentRepository.findAllByItemIdOrderByCreatedDesc(item.getId())).thenReturn(List.of(comment));

        // when
        ItemDto result = itemService.getItemById(item.getId(), owner.getId());

        // then
        assertNotNull(result);
        assertNotNull(result.getLastBooking());
        assertEquals(booking.getId(), result.getLastBooking().getId());
        assertEquals(booking.getBooker().getId(), result.getLastBooking().getBookerId());
        assertNull(result.getNextBooking());
        assertEquals(1, result.getComments().size());
        assertEquals(comment.getText(), result.getComments().get(0).getText());

        verify(bookingRepository).findLastBookingForItem(eq(item.getId()), any(LocalDateTime.class));
        verify(bookingRepository).findNextBookingForItem(eq(item.getId()), any(LocalDateTime.class));
    }

    @Test
    void getItemById_ForNonOwner_ShouldNotShowBookings() {
        // given
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(commentRepository.findAllByItemIdOrderByCreatedDesc(item.getId())).thenReturn(List.of(comment));

        // when
        ItemDto result = itemService.getItemById(item.getId(), booker.getId());

        // then
        assertNotNull(result);
        assertNull(result.getLastBooking());
        assertNull(result.getNextBooking());
        assertEquals(1, result.getComments().size());

        verify(bookingRepository, never()).findLastBookingForItem(anyLong(), any());
        verify(bookingRepository, never()).findNextBookingForItem(anyLong(), any());
    }

    @Test
    void getItemById_ItemNotFound_ShouldThrowException() {
        // given
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.getItemById(999L, owner.getId()));

        assertEquals("Item not found with id: 999", exception.getMessage());
    }

    // ==================== Тесты getAllItemsByOwner ====================

    @Test
    void getAllItemsByOwner_ShouldReturnItemsWithBookingsAndComments() {
        // given
        List<Item> items = List.of(item);
        List<Long> itemIds = List.of(item.getId());
        List<Comment> comments = List.of(comment);
        List<Booking> lastBookings = List.of(booking);
        List<Booking> nextBookings = List.of();

        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(items);
        when(commentRepository.findAllByItemIdInOrderByCreatedDesc(itemIds)).thenReturn(comments);
        when(bookingRepository.findLastBookingsForItems(itemIds, now)).thenReturn(lastBookings);
        when(bookingRepository.findNextBookingsForItems(itemIds, now)).thenReturn(nextBookings);

        // when
        List<ItemDto> results = itemService.getAllItemsByOwner(owner.getId());

        // then
        assertNotNull(results);
        assertEquals(1, results.size());

        ItemDto result = results.get(0);
        assertNotNull(result.getLastBooking());
        assertEquals(booking.getId(), result.getLastBooking().getId());
        assertNull(result.getNextBooking());
        assertEquals(1, result.getComments().size());

        verify(commentRepository).findAllByItemIdInOrderByCreatedDesc(itemIds);
        verify(bookingRepository).findLastBookingsForItems(eq(itemIds), any(LocalDateTime.class));
        verify(bookingRepository).findNextBookingsForItems(eq(itemIds), any(LocalDateTime.class));
    }

    @Test
    void getAllItemsByOwner_NoItems_ShouldReturnEmptyList() {
        // given
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(itemRepository.findAllByOwnerId(owner.getId())).thenReturn(List.of());

        // when
        List<ItemDto> results = itemService.getAllItemsByOwner(owner.getId());

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(commentRepository, never()).findAllByItemIdInOrderByCreatedDesc(any());
        verify(bookingRepository, never()).findLastBookingsForItems(any(), any());
    }

    @Test
    void getAllItemsByOwner_UserNotFound_ShouldThrowException() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.getAllItemsByOwner(999L));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(itemRepository, never()).findAllByOwnerId(any());
    }

    // ==================== Тесты searchItems ====================

    @Test
    void searchItems_WithText_ShouldReturnList() {
        // given
        String searchText = "дрель";
        int from = 0;
        int size = 10;
        List<Item> items = List.of(item);

        when(itemRepository.searchAvailableItems(eq(searchText.toLowerCase()), any(Pageable.class)))
                .thenReturn(items);

        // when
        List<ItemDto> results = itemService.searchItems(searchText, from, size);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(item.getId(), results.get(0).getId());
        assertEquals(item.getName(), results.get(0).getName());
        assertNull(results.get(0).getLastBooking());
        assertNull(results.get(0).getNextBooking());
        assertTrue(results.get(0).getComments() == null || results.get(0).getComments().isEmpty());

        verify(itemRepository).searchAvailableItems(eq(searchText.toLowerCase()), any(Pageable.class));
    }

    @Test
    void searchItems_WithBlankText_ShouldReturnEmptyList() {
        // given
        String searchText = "   ";

        // when
        List<ItemDto> results = itemService.searchItems(searchText, 0, 10);

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(itemRepository, never()).searchAvailableItems(anyString(), any());
    }

    @Test
    void searchItems_WithNullText_ShouldReturnEmptyList() {
        // given

        // when
        List<ItemDto> results = itemService.searchItems(null, 0, 10);

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());

        verify(itemRepository, never()).searchAvailableItems(anyString(), any());
    }

    @Test
    void searchItems_WithNoMatches_ShouldReturnEmptyList() {
        // given
        String searchText = "ноутбук";
        when(itemRepository.searchAvailableItems(eq(searchText.toLowerCase()), any(Pageable.class)))
                .thenReturn(List.of());

        // when
        List<ItemDto> results = itemService.searchItems(searchText, 0, 10);

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ==================== Тесты addComment ====================

    @Test
    void addComment_UserWithCompletedBooking_ShouldSuccess() {
        // given
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(bookingRepository.findFirstByBookerIdAndItemIdAndStatusAndEndBefore(
                eq(booker.getId()), eq(item.getId()), eq(BookingStatus.APPROVED), any(LocalDateTime.class)))
                .thenReturn(Optional.of(booking));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // when
        CommentResponseDto result = itemService.addComment(item.getId(), booker.getId(), commentDto);

        // then
        assertNotNull(result);
        assertEquals(comment.getId(), result.getId());
        assertEquals(comment.getText(), result.getText());
        assertEquals(booker.getName(), result.getAuthorName());

        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_UserWithoutBooking_ShouldThrowException() {
        // given
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(bookingRepository.findFirstByBookerIdAndItemIdAndStatusAndEndBefore(
                eq(booker.getId()), eq(item.getId()), eq(BookingStatus.APPROVED), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> itemService.addComment(item.getId(), booker.getId(), commentDto));

        assertEquals("Only users who have booked this item can leave comments", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_UserNotFound_ShouldThrowException() {
        // given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.addComment(item.getId(), 999L, commentDto));

        assertEquals("User not found with id: 999", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_ItemNotFound_ShouldThrowException() {
        // given
        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(anyLong())).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.addComment(999L, booker.getId(), commentDto));

        assertEquals("Item not found with id: 999", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void addComment_WithBlankText_ShouldThrowException() {
        // given
        commentDto.setText("   ");

        when(userRepository.findById(booker.getId())).thenReturn(Optional.of(booker));
        when(itemRepository.findById(item.getId())).thenReturn(Optional.of(item));
        when(bookingRepository.findFirstByBookerIdAndItemIdAndStatusAndEndBefore(
                eq(booker.getId()), eq(item.getId()), eq(BookingStatus.APPROVED), any(LocalDateTime.class)))
                .thenReturn(Optional.of(booking));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> itemService.addComment(item.getId(), booker.getId(), commentDto));

        assertEquals("Comment text cannot be blank", exception.getMessage());
        verify(commentRepository, never()).save(any());
    }

    // ==================== Тесты getItemsByRequestId ====================

    @Test
    void getItemsByRequestId_ShouldReturnItems() {
        // given
        Long requestId = 1L;
        Item itemWithRequest = new Item();
        itemWithRequest.setId(1L);
        itemWithRequest.setName("Дрель");
        itemWithRequest.setDescription("Электрическая дрель");
        itemWithRequest.setAvailable(true);
        itemWithRequest.setOwner(owner);
        itemWithRequest.setRequest(itemRequest);

        List<Item> items = List.of(itemWithRequest);

        when(itemRequestRepository.findById(requestId)).thenReturn(Optional.of(itemRequest));
        when(itemRepository.findAllByRequestId(requestId)).thenReturn(items);

        // when
        List<ItemDto> results = itemService.getItemsByRequestId(requestId);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(requestId, results.get(0).getRequestId());
    }

    @Test
    void getItemsByRequestId_RequestNotFound_ShouldThrowException() {
        // given
        Long requestId = 999L;
        when(itemRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        // when & then
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> itemService.getItemsByRequestId(requestId));

        assertEquals("Item request not found with id: 999", exception.getMessage());
        verify(itemRepository, never()).findAllByRequestId(any());
    }

    @Test
    void getItemsByRequestId_NoItems_ShouldReturnEmptyList() {
        // given
        Long requestId = 1L;
        when(itemRequestRepository.findById(requestId)).thenReturn(Optional.of(itemRequest));
        when(itemRepository.findAllByRequestId(requestId)).thenReturn(List.of());

        // when
        List<ItemDto> results = itemService.getItemsByRequestId(requestId);

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ==================== Тесты getItemsByRequestIds ====================

    @Test
    void getItemsByRequestIds_ShouldReturnItems() {
        // given
        List<Long> requestIds = List.of(1L, 2L);
        Item item1 = new Item();
        item1.setId(1L);
        item1.setName("Дрель");
        item1.setDescription("Электрическая дрель");
        item1.setAvailable(true);
        item1.setOwner(owner);
        item1.setRequest(itemRequest);

        List<Item> items = List.of(item1);

        when(itemRepository.findAllByRequestIdIn(requestIds)).thenReturn(items);

        // when
        List<ItemDto> results = itemService.getItemsByRequestIds(requestIds);

        // then
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(itemRequest.getId(), results.get(0).getRequestId());
    }

    @Test
    void getItemsByRequestIds_WithNullList_ShouldReturnEmptyList() {
        // given

        // when
        List<ItemDto> results = itemService.getItemsByRequestIds(null);

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(itemRepository, never()).findAllByRequestIdIn(any());
    }

    @Test
    void getItemsByRequestIds_WithEmptyList_ShouldReturnEmptyList() {
        // given
        List<Long> requestIds = List.of();

        // when
        List<ItemDto> results = itemService.getItemsByRequestIds(requestIds);

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(itemRepository, never()).findAllByRequestIdIn(any());
    }

    @Test
    void getItemsByRequestIds_NoItems_ShouldReturnEmptyList() {
        // given
        List<Long> requestIds = List.of(1L, 2L);
        when(itemRepository.findAllByRequestIdIn(requestIds)).thenReturn(List.of());

        // when
        List<ItemDto> results = itemService.getItemsByRequestIds(requestIds);

        // then
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}