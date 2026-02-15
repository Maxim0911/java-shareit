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
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.ItemRequestService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemRequestController.class)
@DisplayName("ItemRequestController MVC Tests")
class ItemRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemRequestService itemRequestService;

    private ItemRequestDto itemRequestDto;
    private ItemRequestResponseDto itemRequestResponseDto;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        itemRequestDto = new ItemRequestDto();
        itemRequestDto.setDescription("Need a drill");

        ItemDto itemDto = ItemDto.builder()
                .id(1L)
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .ownerId(2L)
                .requestId(1L)
                .build();

        itemRequestResponseDto = ItemRequestResponseDto.builder()
                .id(1L)
                .description("Need a drill")
                .requestorId(1L)
                .created(now)
                .items(List.of(itemDto))
                .build();
    }

    @Test
    @DisplayName("POST /requests - should create request successfully")
    void createRequest_Success() throws Exception {
        when(itemRequestService.createRequest(any(ItemRequestDto.class), eq(1L)))
                .thenReturn(itemRequestResponseDto);

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.description", is("Need a drill")))
                .andExpect(jsonPath("$.requestorId", is(1)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id", is(1)));

        verify(itemRequestService, times(1)).createRequest(any(ItemRequestDto.class), eq(1L));
    }

    @Test
    @DisplayName("POST /requests - should return 400 when description is blank")
    void createRequest_BlankDescription_ReturnsBadRequest() throws Exception {
        itemRequestDto.setDescription("");

        mockMvc.perform(post("/requests")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequestDto)))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).createRequest(any(), any());
    }

    @Test
    @DisplayName("POST /requests - should return 400 when userId header is missing")
    void createRequest_MissingUserId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemRequestDto)))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).createRequest(any(), any());
    }

    @Test
    @DisplayName("GET /requests - should get user requests")
    void getUserRequests_Success() throws Exception {
        List<ItemRequestResponseDto> requests = List.of(itemRequestResponseDto);

        when(itemRequestService.getUserRequests(1L)).thenReturn(requests);

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[0].description", is("Need a drill")));

        verify(itemRequestService, times(1)).getUserRequests(1L);
    }

    @Test
    @DisplayName("GET /requests - should return empty list when no requests")
    void getUserRequests_EmptyList_Success() throws Exception {
        when(itemRequestService.getUserRequests(1L)).thenReturn(List.of());

        mockMvc.perform(get("/requests")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(itemRequestService, times(1)).getUserRequests(1L);
    }

    @Test
    @DisplayName("GET /requests/all - should get all requests with pagination")
    void getAllRequests_Success() throws Exception {
        ItemRequestResponseDto request2 = ItemRequestResponseDto.builder()
                .id(2L)
                .description("Need a hammer")
                .requestorId(3L)
                .created(now.minusHours(1))
                .items(List.of())
                .build();

        List<ItemRequestResponseDto> requests = Arrays.asList(itemRequestResponseDto, request2);

        when(itemRequestService.getAllRequests(eq(1L), eq(0), eq(10)))
                .thenReturn(requests);

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(itemRequestService, times(1)).getAllRequests(eq(1L), eq(0), eq(10));
    }

    @Test
    @DisplayName("GET /requests/all - should use default pagination values")
    void getAllRequests_DefaultPagination_Success() throws Exception {
        when(itemRequestService.getAllRequests(eq(1L), eq(0), eq(10)))
                .thenReturn(List.of());

        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk());

        verify(itemRequestService, times(1)).getAllRequests(eq(1L), eq(0), eq(10));
    }

    @Test
    @DisplayName("GET /requests/all - should validate from parameter")
    void getAllRequests_InvalidFrom_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getAllRequests(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /requests/all - should validate size parameter")
    void getAllRequests_InvalidSize_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/requests/all")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getAllRequests(any(), anyInt(), anyInt());
    }

    @Test
    @DisplayName("GET /requests/{requestId} - should get request by id")
    void getRequestById_Success() throws Exception {
        when(itemRequestService.getRequestById(eq(1L), eq(2L)))
                .thenReturn(itemRequestResponseDto);

        mockMvc.perform(get("/requests/1")
                        .header("X-Sharer-User-Id", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.description", is("Need a drill")))
                .andExpect(jsonPath("$.requestorId", is(1)));

        verify(itemRequestService, times(1)).getRequestById(eq(1L), eq(2L));
    }

    @Test
    @DisplayName("GET /requests/{requestId} - should validate requestId path variable")
    void getRequestById_InvalidRequestId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/requests/-1")
                        .header("X-Sharer-User-Id", 2L))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getRequestById(any(), any());
    }

    @Test
    @DisplayName("GET /requests/{requestId} - should validate userId header")
    void getRequestById_InvalidUserId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/requests/1")
                        .header("X-Sharer-User-Id", -1L))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getRequestById(any(), any());
    }

    @Test
    @DisplayName("GET /requests/{requestId} - should return 400 when userId header is missing")
    void getRequestById_MissingUserId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/requests/1"))
                .andExpect(status().isBadRequest());

        verify(itemRequestService, never()).getRequestById(any(), any());
    }
}