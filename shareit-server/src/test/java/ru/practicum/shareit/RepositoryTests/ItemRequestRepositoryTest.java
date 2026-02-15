package ru.practicum.shareit.RepositoryTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ItemRequestRepository Integration Tests")
class ItemRequestRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ItemRequestRepository itemRequestRepository;

    private User user1;
    private User user2;
    private ItemRequest request1;
    private ItemRequest request2;
    private ItemRequest request3;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        user1 = new User();
        user1.setName("User 1");
        user1.setEmail("user1@example.com");
        em.persist(user1);

        user2 = new User();
        user2.setName("User 2");
        user2.setEmail("user2@example.com");
        em.persist(user2);

        request1 = new ItemRequest();
        request1.setDescription("Need a drill");
        request1.setRequestor(user1);
        request1.setCreated(now.minusDays(2));
        em.persist(request1);

        request2 = new ItemRequest();
        request2.setDescription("Need a ladder");
        request2.setRequestor(user1);
        request2.setCreated(now.minusDays(1));
        em.persist(request2);

        request3 = new ItemRequest();
        request3.setDescription("Need a hammer");
        request3.setRequestor(user2);
        request3.setCreated(now);
        em.persist(request3);
    }

    @Test
    @DisplayName("Should save item request")
    void shouldSaveItemRequest() {
        // Given
        ItemRequest newRequest = new ItemRequest();
        newRequest.setDescription("New request");
        newRequest.setRequestor(user2);
        newRequest.setCreated(now);

        // When
        ItemRequest savedRequest = itemRequestRepository.save(newRequest);

        // Then
        assertThat(savedRequest.getId()).isNotNull();
        assertThat(savedRequest.getDescription()).isEqualTo("New request");
        assertThat(savedRequest.getRequestor().getId()).isEqualTo(user2.getId());
        assertThat(savedRequest.getCreated()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should find all requests by requestor id ordered by created desc")
    void shouldFindAllByRequestorIdOrderByCreatedDesc() {
        // When
        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(user1.getId());

        // Then
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getId()).isEqualTo(request2.getId()); // newer first
        assertThat(requests.get(1).getId()).isEqualTo(request1.getId()); // older last
        assertThat(requests.get(0).getCreated()).isAfter(requests.get(1).getCreated());
    }

    @Test
    @DisplayName("Should return empty list when requestor has no requests")
    void shouldReturnEmptyListWhenRequestorHasNoRequests() {
        // Given
        User userWithNoRequests = new User();
        userWithNoRequests.setName("No Requests");
        userWithNoRequests.setEmail("norequests@example.com");
        em.persist(userWithNoRequests);

        // When
        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdOrderByCreatedDesc(userWithNoRequests.getId());

        // Then
        assertThat(requests).isEmpty();
    }

    @Test
    @DisplayName("Should find all requests except those from specified requestor with pagination")
    void shouldFindAllByRequestorIdNot() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created"));

        // When
        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdNot(user1.getId(), pageable);

        // Then
        assertThat(requests).hasSize(1);
        assertThat(requests.get(0).getId()).isEqualTo(request3.getId());
        assertThat(requests.get(0).getRequestor().getId()).isEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should find all requests except specified requestor with pagination - second page")
    void shouldFindAllByRequestorIdNot_SecondPage() {
        // Given
        // Add more requests to test pagination
        for (int i = 0; i < 15; i++) {
            ItemRequest request = new ItemRequest();
            request.setDescription("Request " + i);
            request.setRequestor(user2);
            request.setCreated(now.plusSeconds(i));
            em.persist(request);
        }

        Pageable firstPage = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created"));
        Pageable secondPage = PageRequest.of(1, 10, Sort.by(Sort.Direction.DESC, "created"));

        // When
        List<ItemRequest> firstPageResults = itemRequestRepository.findAllByRequestorIdNot(user1.getId(), firstPage);
        List<ItemRequest> secondPageResults = itemRequestRepository.findAllByRequestorIdNot(user1.getId(), secondPage);

        // Then
        assertThat(firstPageResults).hasSize(10);
        assertThat(secondPageResults).hasSize(6); // 1 (request3) + 15 new = 16 total, 10 on first page, 6 on second
    }

    @Test
    @DisplayName("Should respect sorting order in findAllByRequestorIdNot")
    void shouldRespectSortingInFindAllByRequestorIdNot() {
        // Given
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "created"));

        // When
        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdNot(user2.getId(), pageable);

        // Then
        assertThat(requests).hasSize(2);
        assertThat(requests.get(0).getCreated()).isBefore(requests.get(1).getCreated());
    }

    @Test
    @DisplayName("Should find by id with eager loading of requestor")
    void shouldFindByIdWithRequestor() {
        // When
        ItemRequest found = itemRequestRepository.findById(request1.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getRequestor()).isNotNull();
        assertThat(found.getRequestor().getId()).isEqualTo(user1.getId());
        assertThat(found.getRequestor().getName()).isEqualTo(user1.getName());
    }

    @Test
    @DisplayName("Should handle pagination with zero results")
    void shouldHandlePaginationWithZeroResults() {
        // Given
        User newUser = new User();
        newUser.setName("New User");
        newUser.setEmail("newuser@example.com");
        em.persist(newUser);

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "created"));

        // When
        List<ItemRequest> requests = itemRequestRepository.findAllByRequestorIdNot(newUser.getId(), pageable);

        // Then
        assertThat(requests).hasSize(3); // All requests from user1 and user2
    }

    @Test
    @DisplayName("Should delete item request")
    void shouldDeleteItemRequest() {
        // Given
        Long requestId = request1.getId();

        // When
        itemRequestRepository.deleteById(requestId);
        em.flush();

        // Then
        ItemRequest deleted = em.find(ItemRequest.class, requestId);
        assertThat(deleted).isNull();

        // Verify other requests still exist
        assertThat(em.find(ItemRequest.class, request2.getId())).isNotNull();
        assertThat(em.find(ItemRequest.class, request3.getId())).isNotNull();
    }

    @Test
    @DisplayName("Should update item request")
    void shouldUpdateItemRequest() {
        // Given
        String newDescription = "Updated description";

        // When
        request1.setDescription(newDescription);
        ItemRequest updated = itemRequestRepository.save(request1);
        em.flush();
        em.clear();

        // Then
        ItemRequest found = em.find(ItemRequest.class, request1.getId());
        assertThat(found.getDescription()).isEqualTo(newDescription);
    }
}