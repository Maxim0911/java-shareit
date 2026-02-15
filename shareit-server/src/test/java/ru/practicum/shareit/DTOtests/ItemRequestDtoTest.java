package ru.practicum.shareit.DTOtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.request.dto.ItemRequestDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("ItemRequestDto JSON Test")
class ItemRequestDtoTest {

    @Autowired
    private JacksonTester<ItemRequestDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    private ItemRequestDto itemRequestDto;

    @BeforeEach
    void setUp() {
        itemRequestDto = new ItemRequestDto();
        itemRequestDto.setDescription("Need a drill");
    }

    @Test
    @DisplayName("Should serialize ItemRequestDto to JSON")
    void testSerialize() throws IOException {
        JsonContent<ItemRequestDto> result = json.write(itemRequestDto);

        assertThat(result).hasJsonPathStringValue("$.description");
        assertThat(result).extractingJsonPathStringValue("$.description").isEqualTo("Need a drill");
    }

    @Test
    @DisplayName("Should deserialize JSON to ItemRequestDto")
    void testDeserialize() throws IOException {
        String jsonContent = "{\"description\":\"Need a drill\"}";

        ItemRequestDto deserialized = json.parseObject(jsonContent);

        assertThat(deserialized.getDescription()).isEqualTo("Need a drill");
    }
}