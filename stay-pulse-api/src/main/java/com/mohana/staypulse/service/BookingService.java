package com.mohana.staypulse.service;

import com.mohana.staypulse.dto.request.BookingCreateRequest;
import com.mohana.staypulse.dto.request.BookingUpdateRequest;
import com.mohana.staypulse.dto.response.BookingResponse;

import java.util.List;

/**
 * Booking lifecycle operations. Mutating operations evict the cached analytics results.
 */
public interface BookingService {

    /**
     * Creates a booking.
     *
     * @throws com.mohana.staypulse.exception.InvalidBookingDatesException if check-out is not after check-in
     */
    BookingResponse createBooking(BookingCreateRequest request);

    /** Returns every booking without paging. */
    List<BookingResponse> getAllBookings();

    /**
     * Filtered search; every argument is optional and a blank hotel is treated as no hotel filter.
     * Results are ordered by check-in date, newest first.
     */
    List<BookingResponse> getBookingsWithFilters(
            String hotel,
            com.mohana.staypulse.entity.RoomType roomType,
            com.mohana.staypulse.entity.BookingStatus status,
            java.time.LocalDate checkInFrom,
            java.time.LocalDate checkInTo);

    /**
     * @throws com.mohana.staypulse.exception.ResourceNotFoundException if the booking does not exist
     */
    BookingResponse getBookingById(Long id);

    /**
     * Replaces every editable field of an existing booking.
     *
     * @throws com.mohana.staypulse.exception.ResourceNotFoundException if the booking does not exist
     * @throws com.mohana.staypulse.exception.InvalidBookingDatesException if check-out is not after check-in
     */
    BookingResponse updateBooking(Long id, BookingUpdateRequest request);

    /**
     * @throws com.mohana.staypulse.exception.ResourceNotFoundException if the booking does not exist
     */
    void deleteBooking(Long id);

    /** Case-insensitive exact-name lookup; a blank name yields an empty list. */
    List<BookingResponse> getBookingsByHotel(String hotelName);

    /**
     * Paged listing. Unknown sort fields fall back to {@code createdAt}, the page index is clamped to
     * zero or more and the page size to the range 1-100.
     */
    com.mohana.staypulse.dto.response.PagedResponse<BookingResponse> getPagedBookings(
            int page, int size, String sortBy, String sortDir);
}
