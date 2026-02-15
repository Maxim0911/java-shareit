package ru.practicum.shareit.DTOtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.item.dto.CommentResponseDto;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("CommentResponseDto JSON Test")
class CommentResponseDtoTest {

    @Autowired
    private JacksonTester<CommentResponseDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    private CommentResponseDto commentResponseDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        commentResponseDto = CommentResponseDto.builder()
                .id(1L)
                .text("Great item!")
                .authorName("John Doe")
                .created(now)
                .build();
    }

    @Test
    @DisplayName("Should serialize CommentResponseDto to JSON")
    void testSerialize() throws IOException {
        JsonContent<CommentResponseDto> result = json.write(commentResponseDto);

        assertThat(result).hasJsonPathNumberValue("$.id");
        assertThat(result).hasJsonPathStringValue("$.text");
        assertThat(result).hasJsonPathStringValue("$.authorName");
        assertThat(result).hasJsonPathStringValue("$.created");

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.text").isEqualTo("Great item!");
        assertThat(result).extractingJsonPathStringValue("$.authorName").isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should deserialize JSON to CommentResponseDto")
    void testDeserialize() throws IOException {
        String jsonContent = String.format(
                "{\"id\":1,\"text\":\"Great item!\",\"authorName\":\"John Doe\",\"created\":\"%s\"}",
                now.toString()
        );

        CommentResponseDto deserialized = json.parseObject(jsonContent);

        assertThat(deserialized.getId()).isEqualTo(1L);
        assertThat(deserialized.getText()).isEqualTo("Great item!");
        assertThat(deserialized.getAuthorName()).isEqualTo("John Doe");
        assertThat(deserialized.getCreated()).isNotNull();
    }
}