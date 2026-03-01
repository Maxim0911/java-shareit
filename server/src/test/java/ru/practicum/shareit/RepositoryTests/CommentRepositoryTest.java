package ru.practicum.shareit.RepositoryTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.user.model.User;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CommentRepository Integration Tests")
class CommentRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CommentRepository commentRepository;

    private User author;
    private User anotherUser;
    private Item item1;
    private Item item2;
    private Comment comment1;
    private Comment comment2;
    private Comment comment3;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        author = new User();
        author.setName("Author");
        author.setEmail("author@example.com");
        em.persist(author);

        anotherUser = new User();
        anotherUser.setName("Another User");
        anotherUser.setEmail("another@example.com");
        em.persist(anotherUser);

        User owner = new User();
        owner.setName("Owner");
        owner.setEmail("owner@example.com");
        em.persist(owner);

        item1 = Item.builder()
                .name("Item 1")
                .description("Description 1")
                .available(true)
                .owner(owner)
                .build();
        em.persist(item1);

        item2 = Item.builder()
                .name("Item 2")
                .description("Description 2")
                .available(true)
                .owner(owner)
                .build();
        em.persist(item2);

        comment1 = new Comment();
        comment1.setText("Great item!");
        comment1.setItem(item1);
        comment1.setAuthor(author);
        comment1.setCreated(now.minusDays(2));
        em.persist(comment1);

        comment2 = new Comment();
        comment2.setText("Not bad");
        comment2.setItem(item1);
        comment2.setAuthor(anotherUser);
        comment2.setCreated(now.minusDays(1));
        em.persist(comment2);

        comment3 = new Comment();
        comment3.setText("Awesome!");
        comment3.setItem(item2);
        comment3.setAuthor(author);
        comment3.setCreated(now);
        em.persist(comment3);
    }

    @Test
    @DisplayName("Should save comment")
    void shouldSaveComment() {
        // Given
        Comment newComment = new Comment();
        newComment.setText("New comment");
        newComment.setItem(item1);
        newComment.setAuthor(author);
        newComment.setCreated(now);

        // When
        Comment savedComment = commentRepository.save(newComment);

        // Then
        assertThat(savedComment.getId()).isNotNull();
        assertThat(savedComment.getText()).isEqualTo("New comment");
        assertThat(savedComment.getItem().getId()).isEqualTo(item1.getId());
        assertThat(savedComment.getAuthor().getId()).isEqualTo(author.getId());
    }

    @Test
    @DisplayName("Should find all comments by item id ordered by created desc")
    void shouldFindAllByItemIdOrderByCreatedDesc() {
        // When
        List<Comment> comments = commentRepository.findAllByItemIdOrderByCreatedDesc(item1.getId());

        // Then
        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getId()).isEqualTo(comment2.getId()); // newer first
        assertThat(comments.get(1).getId()).isEqualTo(comment1.getId()); // older last
        assertThat(comments.get(0).getCreated()).isAfter(comments.get(1).getCreated());
    }

    @Test
    @DisplayName("Should return empty list when item has no comments")
    void shouldReturnEmptyListWhenItemHasNoComments() {
        // Given
        Item itemWithNoComments = Item.builder()
                .name("No Comments")
                .description("No comments item")
                .available(true)
                .owner(author)
                .build();
        em.persist(itemWithNoComments);

        // When
        List<Comment> comments = commentRepository.findAllByItemIdOrderByCreatedDesc(itemWithNoComments.getId());

        // Then
        assertThat(comments).isEmpty();
    }

    @Test
    @DisplayName("Should find all comments by multiple item ids ordered by created desc")
    void shouldFindAllByItemIdInOrderByCreatedDesc() {
        // When
        List<Comment> comments = commentRepository.findAllByItemIdInOrderByCreatedDesc(
                List.of(item1.getId(), item2.getId()));

        // Then
        assertThat(comments).hasSize(3);
        // Should be ordered by created desc across all items
        assertThat(comments.get(0).getId()).isEqualTo(comment3.getId()); // newest
        assertThat(comments.get(1).getId()).isEqualTo(comment2.getId());
        assertThat(comments.get(2).getId()).isEqualTo(comment1.getId()); // oldest
    }

    @Test
    @DisplayName("Should handle empty item ids list")
    void shouldHandleEmptyItemIdsList() {
        // When
        List<Comment> comments = commentRepository.findAllByItemIdInOrderByCreatedDesc(List.of());

        // Then
        assertThat(comments).isEmpty();
    }

    @Test
    @DisplayName("Should find comments only for specified item ids")
    void shouldFindCommentsOnlyForSpecifiedItemIds() {
        // When
        List<Comment> comments = commentRepository.findAllByItemIdInOrderByCreatedDesc(List.of(item1.getId()));

        // Then
        assertThat(comments).hasSize(2);
        assertThat(comments).allMatch(c -> c.getItem().getId().equals(item1.getId()));
    }

    @Test
    @DisplayName("Should update comment")
    void shouldUpdateComment() {
        // Given
        String updatedText = "Updated comment text";

        // When
        comment1.setText(updatedText);
        Comment updated = commentRepository.save(comment1);
        em.flush();
        em.clear();

        // Then
        Comment found = em.find(Comment.class, comment1.getId());
        assertThat(found.getText()).isEqualTo(updatedText);
    }

    @Test
    @DisplayName("Should delete comment")
    void shouldDeleteComment() {
        // Given
        Long commentId = comment1.getId();

        // When
        commentRepository.deleteById(commentId);
        em.flush();

        // Then
        Comment deleted = em.find(Comment.class, commentId);
        assertThat(deleted).isNull();

        // Verify other comments still exist
        assertThat(em.find(Comment.class, comment2.getId())).isNotNull();
        assertThat(em.find(Comment.class, comment3.getId())).isNotNull();
    }

    @Test
    @DisplayName("Should verify comment relationships are loaded correctly")
    void shouldVerifyCommentRelationships() {
        // When
        Comment found = commentRepository.findById(comment1.getId()).orElse(null);

        // Then
        assertThat(found).isNotNull();
        assertThat(found.getItem()).isNotNull();
        assertThat(found.getItem().getId()).isEqualTo(item1.getId());
        assertThat(found.getAuthor()).isNotNull();
        assertThat(found.getAuthor().getId()).isEqualTo(author.getId());
    }
}