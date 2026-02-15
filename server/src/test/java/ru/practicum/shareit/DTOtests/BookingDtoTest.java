package ru.practicum.shareit.DTOtests;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
@DisplayName("BookingDto JSON Test")
class BookingDtoTest {

    @Autowired
    private JacksonTester<BookingDto> json;

    @Autowired
    private ObjectMapper objectMapper;

    private BookingDto bookingDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        bookingDto = BookingDto.builder()
                .id(1L)
                .itemId(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .bookerId(2L)
                .status(BookingStatus.WAITING)
                .build();
    }

    @Test
    @DisplayName("Should serialize BookingDto to JSON")
    void testSerialize() throws IOException {
        JsonContent<BookingDto> result = json.write(bookingDto);

        assertThat(result).hasJsonPathNumberValue("$.id");
        assertThat(result).hasJsonPathNumberValue("$.itemId");
        assertThat(result).hasJsonPathStringValue("$.start");
        assertThat(result).hasJsonPathStringValue("$.end");
        assertThat(result).hasJsonPathNumberValue("$.bookerId");
        assertThat(result).hasJsonPathStringValue("$.status");

        assertThat(result).extractingJsonPathNumberValue("$.id").isEqualTo(1);
        assertThat(result).extractingJsonPathStringValue("$.status").isEqualTo("WAITING");
    }

    @Test
    @DisplayName("Should deserialize JSON to BookingDto")
    void testDeserialize() throws IOException {
        String jsonContent = String.format(
                "{\"id\":1,\"itemId\":1,\"start\":\"%s\",\"end\":\"%s\",\"bookerId\":2,\"status\":\"WAITING\"}",
                now.plusDays(1).toString(),
                now.plusDays(2).toString()
        );

        BookingDto deserialized = json.parseObject(jsonContent);

        assertThat(deserialized.getId()).isEqualTo(1L);
        assertThat(deserialized.getItemId()).isEqualTo(1L);
        assertThat(deserialized.getBookerId()).isEqualTo(2L);
        assertThat(deserialized.getStatus()).isEqualTo(BookingStatus.WAITING);
    }

    @Test
    @DisplayName("Should handle optional fields")
    void testOptionalFields() throws IOException {
        BookingDto minimalDto = BookingDto.builder()
                .itemId(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .build();

        JsonContent<BookingDto> result = json.write(minimalDto);

        assertThat(result).hasEmptyJsonPathValue("$.id");
        assertThat(result).hasEmptyJsonPathValue("$.bookerId");
        assertThat(result).hasEmptyJsonPathValue("$.status");
    }
}