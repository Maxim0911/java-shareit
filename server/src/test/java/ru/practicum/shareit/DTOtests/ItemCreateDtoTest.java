package ru.practicum.shareit.DTOtests;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.item.dto.ItemCreateDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("ItemCreateDto JSON Test")
class ItemCreateDtoTest {

    @Autowired
    private JacksonTester<ItemCreateDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    private ItemCreateDto itemCreateDto;

    @BeforeEach
    void setUp() {
        itemCreateDto = ItemCreateDto.builder()
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .requestId(1L)
                .build();
    }

    @Test
    @DisplayName("Should serialize ItemCreateDto to JSON")
    void testSerialize() throws IOException {
        JsonContent<ItemCreateDto> result = json.write(itemCreateDto);

        assertThat(result).hasJsonPathStringValue("$.name");
        assertThat(result).hasJsonPathStringValue("$.description");
        assertThat(result).hasJsonPathBooleanValue("$.available");
        assertThat(result).hasJsonPathNumberValue("$.requestId");

        assertThat(result).extractingJsonPathStringValue("$.name").isEqualTo("Drill");
        assertThat(result).extractingJsonPathBooleanValue("$.available").isTrue();
        assertThat(result).extractingJsonPathNumberValue("$.requestId").isEqualTo(1);
    }

    @Test
    @DisplayName("Should deserialize JSON to ItemCreateDto")
    void testDeserialize() throws IOException {
        String jsonContent = "{\"name\":\"Drill\",\"description\":\"Powerful drill\",\"available\":true,\"requestId\":1}";

        ItemCreateDto deserialized = json.parseObject(jsonContent);

        assertThat(deserialized.getName()).isEqualTo("Drill");
        assertThat(deserialized.getDescription()).isEqualTo("Powerful drill");
        assertThat(deserialized.getAvailable()).isTrue();
        assertThat(deserialized.getRequestId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Should handle null requestId")
    void testNullRequestId() throws IOException {
        itemCreateDto.setRequestId(null);

        JsonContent<ItemCreateDto> result = json.write(itemCreateDto);

        assertThat(result).hasEmptyJsonPathValue("$.requestId");
    }
}