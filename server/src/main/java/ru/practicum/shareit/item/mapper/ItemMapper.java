package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.booking.dto.BookingShortDto;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.CommentResponseDto;
import ru.practicum.shareit.item.dto.ItemUpdateDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.util.List;

public class ItemMapper {

    public static ItemDto toItemDto(Item item) {
        if (item == null) {
            return null;
        }

        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .ownerId(item.getOwner() != null ? item.getOwner().getId() : null)
                .requestId(item.getRequest() != null ? item.getRequest().getId() : null)
                .build();
    }

    public static ItemDto toItemDto(Item item,
                                    Booking lastBooking,
                                    Booking nextBooking,
                                    List<CommentResponseDto> comments) {
        if (item == null) {
            return null;
        }

        BookingShortDto lastBookingShort = null;
        if (lastBooking != null) {
            lastBookingShort = BookingShortDto.builder()
                    .id(lastBooking.getId())
                    .bookerId(lastBooking.getBooker() != null ? lastBooking.getBooker().getId() : null)
                    .build();
        }

        BookingShortDto nextBookingShort = null;
        if (nextBooking != null) {
            nextBookingShort = BookingShortDto.builder()
                    .id(nextBooking.getId())
                    .bookerId(nextBooking.getBooker() != null ? nextBooking.getBooker().getId() : null)
                    .build();
        }

        return toItemDto(item, lastBookingShort, nextBookingShort, comments);
    }

    public static ItemDto toItemDto(Item item,
                                    BookingShortDto lastBooking,
                                    BookingShortDto nextBooking,
                                    List<CommentResponseDto> comments) {
        if (item == null) {
            return null;
        }

        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .ownerId(item.getOwner() != null ? item.getOwner().getId() : null)
                .requestId(item.getRequest() != null ? item.getRequest().getId() : null)
                .lastBooking(lastBooking)    // ✅ Используем BookingShortDto
                .nextBooking(nextBooking)    // ✅ Используем BookingShortDto
                .comments(comments)
                .build();
    }

    public static Item toItem(ItemCreateDto itemCreateDto, User owner) {
        if (itemCreateDto == null) {
            return null;
        }

        Item item = new Item();
        item.setName(itemCreateDto.getName());
        item.setDescription(itemCreateDto.getDescription());
        item.setAvailable(itemCreateDto.getAvailable());
        item.setOwner(owner);

        return item;
    }

    public static void updateItemFromDto(ItemDto itemDto, Item item) {
        if (itemDto == null || item == null) {
            return;
        }

        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }

        if (itemDto.getDescription() != null) {
            item.setDescription(itemDto.getDescription());
        }

        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }
    }

    public static void updateItemFromUpdateDto(ItemUpdateDto itemUpdateDto, Item item) {
        if (itemUpdateDto == null || item == null) {
            return;
        }

        if (itemUpdateDto.getName() != null && !itemUpdateDto.getName().isBlank()) {
            item.setName(itemUpdateDto.getName());
        }

        if (itemUpdateDto.getDescription() != null && !itemUpdateDto.getDescription().isBlank()) {
            item.setDescription(itemUpdateDto.getDescription());
        }

        if (itemUpdateDto.getAvailable() != null) {
            item.setAvailable(itemUpdateDto.getAvailable());
        }
    }
}