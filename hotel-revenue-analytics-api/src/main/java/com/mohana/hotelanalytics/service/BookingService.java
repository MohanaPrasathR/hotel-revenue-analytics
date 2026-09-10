package com.mohana.hotelanalytics.service;

import com.mohana.hotelanalytics.dto.request.BookingCreateRequest;
import com.mohana.hotelanalytics.dto.request.BookingUpdateRequest;
import com.mohana.hotelanalytics.dto.response.BookingResponse;

import java.util.List;

public interface BookingService {

    BookingResponse createBooking(BookingCreateRequest request);

    List<BookingResponse> getAllBookings();

    List<BookingResponse> getBookingsWithFilters(
            String hotel,
            com.mohana.hotelanalytics.entity.RoomType roomType,
            com.mohana.hotelanalytics.entity.BookingStatus status,
            java.time.LocalDate checkInFrom,
            java.time.LocalDate checkInTo);

    BookingResponse getBookingById(Long id);

    BookingResponse updateBooking(Long id, BookingUpdateRequest request);

    void deleteBooking(Long id);

    List<BookingResponse> getBookingsByHotel(String hotelName);

    com.mohana.hotelanalytics.dto.response.PagedResponse<BookingResponse> getPagedBookings(
            int page, int size, String sortBy, String sortDir);
}
