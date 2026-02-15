package ru.practicum.shareit.ControllerTests;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingResponseDto;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@DisplayName("BookingController MVC Tests")
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingService bookingService;

    private BookingDto bookingDto;
    private BookingResponseDto bookingResponseDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        bookingDto = BookingDto.builder()
                .itemId(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .build();

        UserDto booker = new UserDto(2L, "Booker", "booker@example.com");
        ItemDto item = ItemDto.builder()
                .id(1L)
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .ownerId(1L)
                .build();

        bookingResponseDto = BookingResponseDto.builder()
                .id(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .status(BookingStatus.WAITING)
                .booker(booker)
                .item(item)
                .build();
    }

    @Test
    @DisplayName("POST /bookings - should create booking successfully")
    void createBooking_Success() throws Exception {
        when(bookingService.createBooking(any(BookingDto.class), eq(1L))).thenReturn(bookingResponseDto);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("WAITING")))
                .andExpect(jsonPath("$.booker.id", is(2)))
                .andExpect(jsonPath("$.item.id", is(1)));

        verify(bookingService, times(1)).createBooking(any(BookingDto.class), eq(1L));
    }

    @Test
    @DisplayName("POST /bookings - should return 400 when start date is in past")
    void createBooking_PastStart_ReturnsBadRequest() throws Exception {
        bookingDto.setStart(now.minusDays(1));

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingDto)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).createBooking(any(), any());
    }

    @Test
    @DisplayName("POST /bookings - should return 400 when end date is in past")
    void createBooking_PastEnd_ReturnsBadRequest() throws Exception {
        bookingDto.setEnd(now.minusDays(1));

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingDto)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).createBooking(any(), any());
    }

    @Test
    @DisplayName("POST /bookings - should return 400 when itemId is null")
    void createBooking_NullItemId_ReturnsBadRequest() throws Exception {
        bookingDto.setItemId(null);

        mockMvc.perform(post("/bookings")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingDto)))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).createBooking(any(), any());
    }

    @Test
    @DisplayName("PATCH /bookings/{bookingId} - should approve booking")
    void approveBooking_Success() throws Exception {
        BookingResponseDto approvedBooking = BookingResponseDto.builder()
                .id(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .status(BookingStatus.APPROVED)
                .booker(bookingResponseDto.getBooker())
                .item(bookingResponseDto.getItem())
                .build();

        when(bookingService.approveBooking(eq(1L), eq(2L), eq(true)))
                .thenReturn(approvedBooking);

        mockMvc.perform(patch("/bookings/1")
                        .header("X-Sharer-User-Id", 2L)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("APPROVED")));

        verify(bookingService, times(1)).approveBooking(eq(1L), eq(2L), eq(true));
    }

    @Test
    @DisplayName("PATCH /bookings/{bookingId} - should reject booking")
    void approveBooking_Reject_Success() throws Exception {
        BookingResponseDto rejectedBooking = BookingResponseDto.builder()
                .id(1L)
                .start(now.plusDays(1))
                .end(now.plusDays(2))
                .status(BookingStatus.REJECTED)
                .booker(bookingResponseDto.getBooker())
                .item(bookingResponseDto.getItem())
                .build();

        when(bookingService.approveBooking(eq(1L), eq(2L), eq(false)))
                .thenReturn(rejectedBooking);

        mockMvc.perform(patch("/bookings/1")
                        .header("X-Sharer-User-Id", 2L)
                        .param("approved", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("REJECTED")));

        verify(bookingService, times(1)).approveBooking(eq(1L), eq(2L), eq(false));
    }

    @Test
    @DisplayName("GET /bookings/{bookingId} - should get booking by id")
    void getBooking_Success() throws Exception {
        when(bookingService.getBookingById(eq(1L), eq(2L))).thenReturn(bookingResponseDto);

        mockMvc.perform(get("/bookings/1")
                        .header("X-Sharer-User-Id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.status", is("WAITING")));

        verify(bookingService, times(1)).getBookingById(eq(1L), eq(2L));
    }

    @Test
    @DisplayName("GET /bookings - should get user bookings with default params")
    void getUserBookings_DefaultParams_Success() throws Exception {
        List<BookingResponseDto> bookings = List.of(bookingResponseDto);

        when(bookingService.getUserBookings(eq(2L), eq(BookingState.ALL), eq(0), eq(10)))
                .thenReturn(bookings);

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));

        verify(bookingService, times(1)).getUserBookings(eq(2L), eq(BookingState.ALL), eq(0), eq(10));
    }

    @Test
    @DisplayName("GET /bookings - should get user bookings with custom state")
    void getUserBookings_CustomState_Success() throws Exception {
        List<BookingResponseDto> bookings = List.of(bookingResponseDto);

        when(bookingService.getUserBookings(eq(2L), eq(BookingState.WAITING), eq(0), eq(10)))
                .thenReturn(bookings);

        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .param("state", "WAITING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(bookingService, times(1)).getUserBookings(eq(2L), eq(BookingState.WAITING), eq(0), eq(10));
    }

    @Test
    @DisplayName("GET /bookings - should return 400 when state is invalid")
    void getUserBookings_InvalidState_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .param("state", "INVALID_STATE"))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getUserBookings(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /bookings/owner - should get owner bookings")
    void getOwnerBookings_Success() throws Exception {
        List<BookingResponseDto> bookings = List.of(bookingResponseDto);

        when(bookingService.getOwnerBookings(eq(1L), eq(BookingState.ALL), eq(0), eq(10)))
                .thenReturn(bookings);

        mockMvc.perform(get("/bookings/owner")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)));

        verify(bookingService, times(1)).getOwnerBookings(eq(1L), eq(BookingState.ALL), eq(0), eq(10));
    }

    @Test
    @DisplayName("GET /bookings with pagination - should validate from parameter")
    void getUserBookings_InvalidFrom_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getUserBookings(any(), any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /bookings with pagination - should validate size parameter")
    void getUserBookings_InvalidSize_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/bookings")
                        .header("X-Sharer-User-Id", 2L)
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(bookingService, never()).getUserBookings(any(), any(), anyInt(), anyInt());
    }
}