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
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.ItemService;
import ru.practicum.shareit.item.dto.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemController.class)
@DisplayName("ItemController MVC Tests")
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    private ItemCreateDto itemCreateDto;
    private ItemDto itemDto;
    private ItemUpdateDto itemUpdateDto;
    private CommentDto commentDto;
    private CommentResponseDto commentResponseDto;

    @BeforeEach
    void setUp() {
        itemCreateDto = ItemCreateDto.builder()
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .requestId(1L)
                .build();

        itemDto = ItemDto.builder()
                .id(1L)
                .name("Drill")
                .description("Powerful drill")
                .available(true)
                .ownerId(1L)
                .requestId(1L)
                .comments(List.of())
                .build();

        itemUpdateDto = ItemUpdateDto.builder()
                .name("Updated Drill")
                .description("Updated description")
                .available(false)
                .build();

        commentDto = new CommentDto();
        commentDto.setText("Great item!");

        commentResponseDto = CommentResponseDto.builder()
                .id(1L)
                .text("Great item!")
                .authorName("John Doe")
                .created(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("POST /items - should create item successfully")
    void createItem_Success() throws Exception {
        when(itemService.createItem(any(ItemCreateDto.class), eq(1L))).thenReturn(itemDto);

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemCreateDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Drill")))
                .andExpect(jsonPath("$.available", is(true)));

        verify(itemService, times(1)).createItem(any(ItemCreateDto.class), eq(1L));
    }

    @Test
    @DisplayName("POST /items - should return 400 when name is blank")
    void createItem_BlankName_ReturnsBadRequest() throws Exception {
        itemCreateDto.setName("");

        mockMvc.perform(post("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemCreateDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).createItem(any(), any());
    }

    @Test
    @DisplayName("POST /items - should return 400 when userId header is missing")
    void createItem_MissingUserId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemCreateDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).createItem(any(), any());
    }

    @Test
    @DisplayName("PATCH /items/{itemId} - should update item successfully")
    void updateItem_Success() throws Exception {
        ItemDto updatedItem = ItemDto.builder()
                .id(1L)
                .name("Updated Drill")
                .description("Updated description")
                .available(false)
                .ownerId(1L)
                .build();

        when(itemService.updateItem(eq(1L), any(ItemUpdateDto.class), eq(1L))).thenReturn(updatedItem);

        mockMvc.perform(patch("/items/1")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemUpdateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Updated Drill")))
                .andExpect(jsonPath("$.available", is(false)));

        verify(itemService, times(1)).updateItem(eq(1L), any(ItemUpdateDto.class), eq(1L));
    }

    @Test
    @DisplayName("GET /items/{itemId} - should return item by id")
    void getItemById_Success() throws Exception {
        when(itemService.getItemById(1L, 1L)).thenReturn(itemDto);

        mockMvc.perform(get("/items/1")
                        .header("X-Sharer-User-Id", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Drill")));

        verify(itemService, times(1)).getItemById(1L, 1L);
    }

    @Test
    @DisplayName("GET /items - should return all items for owner")
    void getAllItemsByOwner_Success() throws Exception {
        ItemDto item2 = ItemDto.builder()
                .id(2L)
                .name("Hammer")
                .description("Heavy hammer")
                .available(true)
                .ownerId(1L)
                .build();

        List<ItemDto> items = Arrays.asList(itemDto, item2);

        when(itemService.getAllItemsByOwner(1L)).thenReturn(items);

        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(1)))
                .andExpect(jsonPath("$[1].id", is(2)));

        verify(itemService, times(1)).getAllItemsByOwner(1L);
    }

    @Test
    @DisplayName("GET /items/search - should search items")
    void searchItems_Success() throws Exception {
        List<ItemDto> searchResults = List.of(itemDto);

        when(itemService.searchItems(eq("drill"), eq(0), eq(10))).thenReturn(searchResults);

        mockMvc.perform(get("/items/search")
                        .param("text", "drill")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Drill")));

        verify(itemService, times(1)).searchItems(eq("drill"), eq(0), eq(10));
    }

    @Test
    @DisplayName("GET /items/search - should return empty list when text is empty")
    void searchItems_EmptyText_ReturnsEmptyList() throws Exception {
        when(itemService.searchItems(eq(""), eq(0), eq(10))).thenReturn(List.of());

        mockMvc.perform(get("/items/search")
                        .param("text", "")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(itemService, times(1)).searchItems(eq(""), eq(0), eq(10));
    }

    @Test
    @DisplayName("POST /items/{itemId}/comment - should add comment successfully")
    void addComment_Success() throws Exception {
        when(itemService.addComment(eq(1L), eq(1L), any(CommentDto.class)))
                .thenReturn(commentResponseDto);

        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.text", is("Great item!")))
                .andExpect(jsonPath("$.authorName", is("John Doe")));

        verify(itemService, times(1)).addComment(eq(1L), eq(1L), any(CommentDto.class));
    }

    @Test
    @DisplayName("POST /items/{itemId}/comment - should return 400 when comment text is blank")
    void addComment_BlankText_ReturnsBadRequest() throws Exception {
        commentDto.setText("");

        mockMvc.perform(post("/items/1/comment")
                        .header("X-Sharer-User-Id", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).addComment(any(), any(), any());
    }

    @Test
    @DisplayName("GET /items with pagination - should validate from parameter")
    void getAllItemsByOwner_InvalidFrom_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).getAllItemsByOwner(any());
    }

    @Test
    @DisplayName("GET /items with pagination - should validate size parameter")
    void getAllItemsByOwner_InvalidSize_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/items")
                        .header("X-Sharer-User-Id", 1L)
                        .param("from", "0")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(itemService, never()).getAllItemsByOwner(any());
    }
}