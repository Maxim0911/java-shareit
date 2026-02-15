package ru.practicum.shareit.RepositoryTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ItemRepository Integration Tests")
class ItemRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ItemRepository itemRepository;

    private User owner;
    private User requester;
    private Item item1;
    private Item item2;
    private Item item3;
    private ItemRequest request;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@example.com");
        em.persist(owner);

        requester = new User();
        requester.setName("Requester");
        requester.setEmail("requester@example.com");
        em.persist(requester);

        request = new ItemRequest();
        request.setDescription("Need an item");
        request.setRequestor(requester);
        request.setCreated(LocalDateTime.now());
        em.persist(request);

        item1 = Item.builder()
                .name("Drill")
                .description("Powerful electric drill")
                .available(true)
                .owner(owner)
                .request(request)
                .build();
        em.persist(item1);

        item2 = Item.builder()
                .name("Hammer")
                .description("Heavy duty hammer")
                .available(true)
                .owner(owner)
                .build();
        em.persist(item2);

        item3 = Item.builder()
                .name("Unavailable Drill")
                .description("Old broken drill")
                .available(false)
                .owner(owner)
                .build();
        em.persist(item3);
    }

    @Test
    @DisplayName("Should save item")
    void shouldSaveItem() {
        // Given
        Item newItem = Item.builder()
                .name("Saw")
                .description("Sharp saw")
                .available(true)
                .owner(owner)
                .build();

        // When
        Item savedItem = itemRepository.save(newItem);

        // Then
        assertThat(savedItem.getId()).isNotNull();
        assertThat(savedItem.getName()).isEqualTo(newItem.getName());
        assertThat(savedItem.getOwner().getId()).isEqualTo(owner.getId());
    }

    @Test
    @DisplayName("Should find items by owner id")
    void shouldFindItemsByOwnerId() {
        // When
        List<Item> items = itemRepository.findAllByOwnerId(owner.getId());

        // Then
        assertThat(items).hasSize(3);
        assertThat(items).extracting(Item::getName)
                .containsExactlyInAnyOrder("Drill", "Hammer", "Unavailable Drill");
    }

    @Test
    @DisplayName("Should search available items by text in name")
    void shouldSearchAvailableItemsByName() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Item> results = itemRepository.searchAvailableItems("drill", pageable);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Drill");
        assertThat(results.get(0).getAvailable()).isTrue();
    }

    @Test
    @DisplayName("Should search available items by text in description")
    void shouldSearchAvailableItemsByDescription() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Item> results = itemRepository.searchAvailableItems("electric", pageable);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Drill");
        assertThat(results.get(0).getDescription()).contains("electric");
    }

    @Test
    @DisplayName("Should search available items case insensitive")
    void shouldSearchAvailableItemsCaseInsensitive() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Item> lowerCaseResults = itemRepository.searchAvailableItems("drill", pageable);
        List<Item> upperCaseResults = itemRepository.searchAvailableItems("DRILL", pageable);
        List<Item> mixedCaseResults = itemRepository.searchAvailableItems("DrIlL", pageable);

        // Then
        assertThat(lowerCaseResults).hasSize(1);
        assertThat(upperCaseResults).hasSize(1);
        assertThat(mixedCaseResults).hasSize(1);
    }

    @Test
    @DisplayName("Should search available items with partial text")
    void shouldSearchAvailableItemsWithPartialText() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Item> results = itemRepository.searchAvailableItems("dri", pageable);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Drill");
    }

    @Test
    @DisplayName("Should not return unavailable items in search")
    void shouldNotReturnUnavailableItemsInSearch() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Item> results = itemRepository.searchAvailableItems("unavailable", pageable);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when search text not found")
    void shouldReturnEmptyListWhenSearchTextNotFound() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<Item> results = itemRepository.searchAvailableItems("nonexistent", pageable);

        // Then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should respect pagination in search")
    void shouldRespectPaginationInSearch() {
        // Given
        // Добавляем еще несколько доступных вещей
        for (int i = 0; i < 15; i++) {
            Item item = Item.builder()
                    .name("Item " + i)
                    .description("Description " + i)
                    .available(true)
                    .owner(owner)
                    .build();
            em.persist(item);
        }

        Pageable firstPage = PageRequest.of(0, 5);
        Pageable secondPage = PageRequest.of(1, 5);

        // When
        List<Item> firstPageResults = itemRepository.searchAvailableItems("Item", firstPage);
        List<Item> secondPageResults = itemRepository.searchAvailableItems("Item", secondPage);

        // Then
        assertThat(firstPageResults).hasSize(5);
        assertThat(secondPageResults).hasSize(5);
        assertThat(firstPageResults).isNotEqualTo(secondPageResults);
    }

    @Test
    @DisplayName("Should find items by request ids")
    void shouldFindItemsByRequestIds() {
        // When
        List<Item> items = itemRepository.findAllByRequestIdIn(List.of(request.getId()));

        // Then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getName()).isEqualTo("Drill");
        assertThat(items.get(0).getRequest().getId()).isEqualTo(request.getId());
    }

    @Test
    @DisplayName("Should find items by single request id")
    void shouldFindItemsByRequestId() {
        // When
        List<Item> items = itemRepository.findAllByRequestId(request.getId());

        // Then
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getName()).isEqualTo("Drill");
        assertThat(items.get(0).getRequest().getId()).isEqualTo(request.getId());
    }

    @Test
    @DisplayName("Should handle empty request ids list")
    void shouldHandleEmptyRequestIdsList() {
        // When
        List<Item> items = itemRepository.findAllByRequestIdIn(List.of());

        // Then
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Should return empty list when request id not found")
    void shouldReturnEmptyListWhenRequestIdNotFound() {
        // When
        List<Item> items = itemRepository.findAllByRequestId(999L);

        // Then
        assertThat(items).isEmpty();
    }

    @Test
    @DisplayName("Should find item by id with relationships loaded")
    void shouldFindItemByIdWithRelationships() {
        // When
        Item found = itemRepository.findById(item1.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getOwner()).isNotNull();
        assertThat(found.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(found.getRequest()).isNotNull();
        assertThat(found.getRequest().getId()).isEqualTo(request.getId());
    }

    @Test
    @DisplayName("Should update item")
    void shouldUpdateItem() {
        // Given
        String updatedName = "Updated Drill";
        String updatedDescription = "Updated description";

        // When
        item1.setName(updatedName);
        item1.setDescription(updatedDescription);
        Item updated = itemRepository.save(item1);
        em.flush();
        em.clear();

        // Then
        Item found = em.find(Item.class, item1.getId());
        assertThat(found.getName()).isEqualTo(updatedName);
        assertThat(found.getDescription()).isEqualTo(updatedDescription);
    }

    @Test
    @DisplayName("Should delete item")
    void shouldDeleteItem() {
        // Given
        Long itemId = item1.getId();

        // When
        itemRepository.deleteById(itemId);
        em.flush();

        // Then
        Item deleted = em.find(Item.class, itemId);
        assertThat(deleted).isNull();

        // Verify other items still exist
        assertThat(em.find(Item.class, item2.getId())).isNotNull();
        assertThat(em.find(Item.class, item3.getId())).isNotNull();
    }
}