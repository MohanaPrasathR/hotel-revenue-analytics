package com.mohana.hotelanalytics.controller;

import com.mohana.hotelanalytics.dto.request.BookingCreateRequest;
import com.mohana.hotelanalytics.dto.request.BookingUpdateRequest;
import com.mohana.hotelanalytics.dto.response.BookingResponse;
import com.mohana.hotelanalytics.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingCreateRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings(
            @RequestParam(required = false) String hotel,
            @RequestParam(required = false) com.mohana.hotelanalytics.entity.RoomType roomType,
            @RequestParam(required = false) com.mohana.hotelanalytics.entity.BookingStatus status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate checkInFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate checkInTo) {
        if (hotel != null || roomType != null || status != null || checkInFrom != null || checkInTo != null) {
            List<BookingResponse> bookings = bookingService.getBookingsWithFilters(hotel, roomType, status, checkInFrom, checkInTo);
            return ResponseEntity.ok(bookings);
        }
        List<BookingResponse> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        BookingResponse response = bookingService.getBookingById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingUpdateRequest request) {
        BookingResponse response = bookingService.updateBooking(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/hotel/{hotelName}")
    public ResponseEntity<List<BookingResponse>> getBookingsByHotel(@PathVariable String hotelName) {
        List<BookingResponse> bookings = bookingService.getBookingsByHotel(hotelName);
        return ResponseEntity.ok(bookings);
    }
}
