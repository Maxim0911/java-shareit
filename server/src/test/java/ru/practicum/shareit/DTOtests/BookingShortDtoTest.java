package ru.practicum.shareit.DTOtests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookingShortDto;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("BookingShortDto JSON Test")
class BookingShortDtoTest {

    @Autowired
    private JacksonTester<BookingShortDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    private BookingShortDto bookingShortDto;

    @BeforeEach
    void setUp() {
        bookingShortDto = BookingShortDto.builder()
                .id(1L)
                .bookerId(2L)
                .build();
    }

    @Test
    @DisplayName("Should serialize BookingShortDto to JSON")
    void testSerialize() throws IOException {
        JsonContent<BookingShortDto> result = json.write(bookingShortDto);

        assertThat(result).hasJsonPathNumberValue("$.id");
        assertThat(result).hasJsonPathNumberValue("$.bookerId");

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathNumberValue("$.bookerId").isEqualTo(2);
    }

    @Test
    @DisplayName("Should deserialize JSON to BookingShortDto")
    void testDeserialize() throws IOException {
        String jsonContent = "{\"id\":1,\"bookerId\":2}";

        BookingShortDto deserialized = json.parseObject(jsonContent);

        assertThat(deserialized.getId()).isEqualTo(1L);
        assertThat(deserialized.getBookerId()).isEqualTo(2L);
    }
}