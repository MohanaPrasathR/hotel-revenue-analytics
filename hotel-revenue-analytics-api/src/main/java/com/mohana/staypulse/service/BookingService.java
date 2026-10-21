package com.mohana.staypulse.service;

import com.mohana.staypulse.dto.request.BookingCreateRequest;
import com.mohana.staypulse.dto.request.BookingUpdateRequest;
import com.mohana.staypulse.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request);

    List<BookingResponse> getAllBookings();

    List<BookingResponse> getBookingsWithFilters(
            String hotel,
            com.mohana.staypulse.entity.RoomType roomType,
            com.mohana.staypulse.entity.BookingStatus status,
            java.time.LocalDate checkInFrom,
            java.time.LocalDate checkInTo);

    BookingResponse getBookingById(Long id);

    BookingResponse updateBooking(Long id, BookingUpdateRequest request);

    void deleteBooking(Long id);

    List<BookingResponse> getBookingsByHotel(String hotelName);

    com.mohana.staypulse.dto.response.PagedResponse<BookingResponse> getPagedBookings(
            int page, int size, String sortBy, String sortDir);
}
