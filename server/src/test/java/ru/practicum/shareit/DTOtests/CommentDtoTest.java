package ru.practicum.shareit.DTOtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.item.dto.CommentDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("CommentDto JSON Test")
class CommentDtoTest {

    @Autowired
    private JacksonTester<CommentDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    private CommentDto commentDto;

    @BeforeEach
    void setUp() {
        commentDto = new CommentDto();
        commentDto.setText("Great item!");
    }

    @Test
    @DisplayName("Should serialize CommentDto to JSON")
    void testSerialize() throws IOException {
        JsonContent<CommentDto> result = json.write(commentDto);

        assertThat(result).hasJsonPathStringValue("$.text");
        assertThat(result).extractingJsonPathStringValue("$.text").isEqualTo("Great item!");
    }

    @Test
    @DisplayName("Should deserialize JSON to CommentDto")
    void testDeserialize() throws IOException {
        String jsonContent = "{\"text\":\"Great item!\"}";

        CommentDto deserialized = json.parseObject(jsonContent);

        assertThat(deserialized.getText()).isEqualTo("Great item!");
    }
}