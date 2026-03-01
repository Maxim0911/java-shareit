package ru.practicum.shareit.ServiceTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.exception.ValidationException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.ItemRequestServiceImpl;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ItemRequestService Unit Tests")
class ItemRequestServiceImplTest {

    @Mock
    private ItemRequestRepository itemRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemRequestServiceImpl itemRequestService;

    @Captor
    private ArgumentCaptor<ItemRequest> itemRequestCaptor;

    @Captor
    private ArgumentCaptor<List<Long>> requestIdsCaptor;

    private User user;
    private User anotherUser;
    private ItemRequest itemRequest;
    private ItemRequest anotherRequest;
    private Item item;
    private ItemDto itemDto;
    private ItemRequestDto itemRequestDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        user = new User(1L, "John Doe", "john@example.com");
        anotherUser = new User(2L, "Jane Smith", "jane@example.com");

        itemRequest = new ItemRequest();
        itemRequest.setId(1L);
        itemRequest.setDescription("Need a drill");
        itemRequest.setRequestor(user);
        itemRequest.setCreated(now);

        anotherRequest = new ItemRequest();
        anotherRequest.setId(2L);
        anotherRequest.setDescription("Need a ladder");
        anotherRequest.setRequestor(anotherUser);
        anotherRequest.setCreated(now.minusHours(1));

        item = new Item();
        item.setId(1L);
        item.setName("Drill");
        item.setDescription("Powerful drill");
        item.setAvailable(true);
        item.setOwner(anotherUser);
        item.setRequest(itemRequest);

        itemDto = ItemDto.builder()
                .id(1L)
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .ownerId(2L)
                .requestId(1L)
                .build();

        itemRequestDto = new ItemRequestDto();
        itemRequestDto.setDescription("Need a drill");
    }

    @Test
    @DisplayName("Should create request successfully")
    void createRequest_Success() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.save(any(ItemRequest.class))).thenReturn(itemRequest);

        // When
        ItemRequestResponseDto result = itemRequestService.createRequest(itemRequestDto, user.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(itemRequest.getId());
        assertThat(result.getDescription()).isEqualTo(itemRequest.getDescription());
        assertThat(result.getRequestorId()).isEqualTo(user.getId());
        assertThat(result.getCreated()).isEqualTo(itemRequest.getCreated());
        assertThat(result.getItems()).isEmpty();

        verify(userRepository).findById(user.getId());
        verify(itemRequestRepository).save(itemRequestCaptor.capture());

        ItemRequest capturedRequest = itemRequestCaptor.getValue();
        assertThat(capturedRequest.getDescription()).isEqualTo(itemRequestDto.getDescription());
        assertThat(capturedRequest.getRequestor()).isEqualTo(user);
        assertThat(capturedRequest.getCreated()).isNotNull();
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found during creation")
    void createRequest_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> itemRequestService.createRequest(itemRequestDto, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(userRepository).findById(999L);
        verify(itemRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when description is null")
    void createRequest_NullDescription_ThrowsException() {
        // Given
        itemRequestDto.setDescription(null);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // When/Then
        assertThatThrownBy(() -> itemRequestService.createRequest(itemRequestDto, user.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Description cannot be blank");

        verify(itemRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw ValidationException when description is blank")
    void createRequest_BlankDescription_ThrowsException() {
        // Given
        itemRequestDto.setDescription("   ");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // When/Then
        assertThatThrownBy(() -> itemRequestService.createRequest(itemRequestDto, user.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Description cannot be blank");

        verify(itemRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should get user requests successfully with items")
    void getUserRequests_SuccessWithItems() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(user.getId()))
                .thenReturn(List.of(itemRequest));
        when(itemRepository.findAllByRequestIdIn(List.of(itemRequest.getId())))
                .thenReturn(List.of(item));

        // When
        List<ItemRequestResponseDto> results = itemRequestService.getUserRequests(user.getId());

        // Then
        assertThat(results).hasSize(1);
        ItemRequestResponseDto result = results.get(0);
        assertThat(result.getId()).isEqualTo(itemRequest.getId());
        assertThat(result.getDescription()).isEqualTo(itemRequest.getDescription());
        assertThat(result.getRequestorId()).isEqualTo(user.getId());
        assertThat(result.getItems()).hasSize(1);

        ItemDto resultItem = result.getItems().get(0);
        assertThat(resultItem.getId()).isEqualTo(itemDto.getId());
        assertThat(resultItem.getName()).isEqualTo(itemDto.getName());
        assertThat(resultItem.getRequestId()).isEqualTo(itemDto.getRequestId());

        verify(userRepository).findById(user.getId());
        verify(itemRequestRepository).findAllByRequestorIdOrderByCreatedDesc(user.getId());
        verify(itemRepository).findAllByRequestIdIn(List.of(itemRequest.getId()));
    }

    @Test
    @DisplayName("Should get user requests successfully without items")
    void getUserRequests_SuccessWithoutItems() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(user.getId()))
                .thenReturn(List.of(itemRequest));
        when(itemRepository.findAllByRequestIdIn(List.of(itemRequest.getId())))
                .thenReturn(Collections.emptyList());

        // When
        List<ItemRequestResponseDto> results = itemRequestService.getUserRequests(user.getId());

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getItems()).isEmpty();

        verify(itemRepository).findAllByRequestIdIn(List.of(itemRequest.getId()));
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found during getUserRequests")
    void getUserRequests_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> itemRequestService.getUserRequests(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(itemRequestRepository, never()).findAllByRequestorIdOrderByCreatedDesc(anyLong());
    }

    @Test
    @DisplayName("Should get all requests with pagination")
    void getAllRequests_Success() {
        // Given
        int from = 0;
        int size = 10;
        Pageable expectedPageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "created"));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdNot(user.getId(), expectedPageable))
                .thenReturn(List.of(anotherRequest));
        when(itemRepository.findAllByRequestIdIn(List.of(anotherRequest.getId())))
                .thenReturn(Collections.emptyList());

        // When
        List<ItemRequestResponseDto> results = itemRequestService.getAllRequests(user.getId(), from, size);

        // Then
        assertThat(results).hasSize(1);
        ItemRequestResponseDto result = results.get(0);
        assertThat(result.getId()).isEqualTo(anotherRequest.getId());
        assertThat(result.getDescription()).isEqualTo(anotherRequest.getDescription());
        assertThat(result.getRequestorId()).isEqualTo(anotherUser.getId());
        assertThat(result.getItems()).isEmpty();

        verify(itemRequestRepository).findAllByRequestorIdNot(eq(user.getId()), any(Pageable.class));
        verify(itemRepository).findAllByRequestIdIn(List.of(anotherRequest.getId()));
    }

    @Test
    @DisplayName("Should get all requests with pagination (non-first page)")
    void getAllRequests_WithPagination_SecondPage() {
        // Given
        int from = 10;
        int size = 10;
        Pageable expectedPageable = PageRequest.of(1, size, Sort.by(Sort.Direction.DESC, "created"));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdNot(user.getId(), expectedPageable))
                .thenReturn(Collections.emptyList());
        when(itemRepository.findAllByRequestIdIn(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        List<ItemRequestResponseDto> results = itemRequestService.getAllRequests(user.getId(), from, size);

        // Then
        assertThat(results).isEmpty();
        verify(itemRequestRepository).findAllByRequestorIdNot(eq(user.getId()), any(Pageable.class));
        verify(itemRepository).findAllByRequestIdIn(Collections.emptyList());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found during getAllRequests")
    void getAllRequests_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> itemRequestService.getAllRequests(999L, 0, 10))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(itemRequestRepository, never()).findAllByRequestorIdNot(anyLong(), any());
    }

    @Test
    @DisplayName("Should get request by id successfully")
    void getRequestById_Success() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findById(itemRequest.getId())).thenReturn(Optional.of(itemRequest));
        when(itemRepository.findAllByRequestId(itemRequest.getId())).thenReturn(List.of(item));

        // When
        ItemRequestResponseDto result = itemRequestService.getRequestById(itemRequest.getId(), user.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(itemRequest.getId());
        assertThat(result.getDescription()).isEqualTo(itemRequest.getDescription());
        assertThat(result.getRequestorId()).isEqualTo(user.getId());
        assertThat(result.getItems()).hasSize(1);

        ItemDto resultItem = result.getItems().get(0);
        assertThat(resultItem.getId()).isEqualTo(item.getId());
        assertThat(resultItem.getRequestId()).isEqualTo(itemRequest.getId());

        verify(userRepository).findById(user.getId());
        verify(itemRequestRepository).findById(itemRequest.getId());
        verify(itemRepository).findAllByRequestId(itemRequest.getId());
    }

    @Test
    @DisplayName("Should get request by id without items")
    void getRequestById_WithoutItems_Success() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findById(itemRequest.getId())).thenReturn(Optional.of(itemRequest));
        when(itemRepository.findAllByRequestId(itemRequest.getId())).thenReturn(Collections.emptyList());

        // When
        ItemRequestResponseDto result = itemRequestService.getRequestById(itemRequest.getId(), user.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getItems()).isEmpty();

        verify(itemRepository).findAllByRequestId(itemRequest.getId());
    }

    @Test
    @DisplayName("Should throw NotFoundException when user not found during getRequestById")
    void getRequestById_UserNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> itemRequestService.getRequestById(1L, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(itemRequestRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw NotFoundException when request not found during getRequestById")
    void getRequestById_RequestNotFound_ThrowsException() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findById(anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> itemRequestService.getRequestById(999L, user.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Item request not found with id: 999");

        verify(itemRepository, never()).findAllByRequestId(anyLong());
    }

    @Test
    @DisplayName("Should handle empty requests list for user - repository called with empty list")
    void getUserRequests_EmptyList() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(user.getId()))
                .thenReturn(Collections.emptyList());
        when(itemRepository.findAllByRequestIdIn(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        List<ItemRequestResponseDto> results = itemRequestService.getUserRequests(user.getId());

        // Then
        assertThat(results).isEmpty();
        verify(itemRepository).findAllByRequestIdIn(Collections.emptyList());
    }

    @Test
    @DisplayName("Should handle empty requests list for getAllRequests - repository called with empty list")
    void getAllRequests_EmptyList() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdNot(eq(user.getId()), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(itemRepository.findAllByRequestIdIn(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        List<ItemRequestResponseDto> results = itemRequestService.getAllRequests(user.getId(), 0, 10);

        // Then
        assertThat(results).isEmpty();
        verify(itemRepository).findAllByRequestIdIn(Collections.emptyList());
    }

    @Test
    @DisplayName("Should handle multiple requests with items correctly")
    void getUserRequests_MultipleRequestsWithItems() {
        // Given
        ItemRequest request2 = new ItemRequest();
        request2.setId(3L);
        request2.setDescription("Need a saw");
        request2.setRequestor(user);
        request2.setCreated(now.minusHours(2));

        Item item2 = new Item();
        item2.setId(2L);
        item2.setName("Saw");
        item2.setDescription("Sharp saw");
        item2.setAvailable(true);
        item2.setOwner(anotherUser);
        item2.setRequest(request2);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(user.getId()))
                .thenReturn(List.of(itemRequest, request2));
        when(itemRepository.findAllByRequestIdIn(List.of(1L, 3L)))
                .thenReturn(List.of(item, item2));

        // When
        List<ItemRequestResponseDto> results = itemRequestService.getUserRequests(user.getId());

        // Then
        assertThat(results).hasSize(2);

        // First request (most recent) should have one item
        assertThat(results.get(0).getItems()).hasSize(1);
        assertThat(results.get(0).getItems().get(0).getId()).isEqualTo(1L);

        // Second request should have one item
        assertThat(results.get(1).getItems()).hasSize(1);
        assertThat(results.get(1).getItems().get(0).getId()).isEqualTo(2L);

        verify(itemRepository).findAllByRequestIdIn(List.of(1L, 3L));
    }

    @Test
    @DisplayName("Should verify that findAllByRequestIdIn is called even with empty list in getUserRequests")
    void getUserRequests_VerifyRepositoryCallWithEmptyList() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(user.getId()))
                .thenReturn(Collections.emptyList());
        when(itemRepository.findAllByRequestIdIn(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        itemRequestService.getUserRequests(user.getId());

        // Then
        verify(itemRepository).findAllByRequestIdIn(requestIdsCaptor.capture());
        assertThat(requestIdsCaptor.getValue()).isEmpty();
    }

    @Test
    @DisplayName("Should verify that findAllByRequestIdIn is called even with empty list in getAllRequests")
    void getAllRequests_VerifyRepositoryCallWithEmptyList() {
        // Given
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(itemRequestRepository.findAllByRequestorIdNot(eq(user.getId()), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(itemRepository.findAllByRequestIdIn(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When
        itemRequestService.getAllRequests(user.getId(), 0, 10);

        // Then
        verify(itemRepository).findAllByRequestIdIn(requestIdsCaptor.capture());
        assertThat(requestIdsCaptor.getValue()).isEmpty();
    }
}