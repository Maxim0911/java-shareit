package ru.practicum.shareit.item.mapper;

import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemCreateDto;
import ru.practicum.shareit.item.dto.CommentResponseDto;
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
                                    List<CommentResponseDto> comments) { // ИЗМЕНИЛ тип
        if (item == null) {
            return null;
        }

        ItemDto.BookingInfo lastBookingInfo = null;
        if (lastBooking != null) {
            lastBookingInfo = ItemDto.BookingInfo.builder()
                    .id(lastBooking.getId())
                    .bookerId(lastBooking.getBooker() != null ? lastBooking.getBooker().getId() : null)
                    .build();
        }

        ItemDto.BookingInfo nextBookingInfo = null;
        if (nextBooking != null) {
            nextBookingInfo = ItemDto.BookingInfo.builder()
                    .id(nextBooking.getId())
                    .bookerId(nextBooking.getBooker() != null ? nextBooking.getBooker().getId() : null)
                    .build();
        }

        return ItemDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .available(item.getAvailable())
                .ownerId(item.getOwner() != null ? item.getOwner().getId() : null)
                .requestId(item.getRequest() != null ? item.getRequest().getId() : null)
                .lastBooking(lastBookingInfo)
                .nextBooking(nextBookingInfo)
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
}