package com.mohana.staypulse.service;

import com.mohana.staypulse.dto.request.BookingCreateRequest;
import com.mohana.staypulse.dto.request.BookingUpdateRequest;
import com.mohana.staypulse.dto.response.BookingResponse;
import com.mohana.staypulse.entity.Booking;
import com.mohana.staypulse.entity.BookingStatus;
import com.mohana.staypulse.entity.RoomType;
import com.mohana.staypulse.exception.InvalidBookingDatesException;
import com.mohana.staypulse.exception.ResourceNotFoundException;
import com.mohana.staypulse.repository.BookingRepository;
import com.mohana.staypulse.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private Booking sampleBooking;
    private BookingCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        sampleBooking = Booking.builder()
                .id(1L)
                .hotelName("Grand Hyatt")
                .guestName("John Doe")
                .checkInDate(LocalDate.of(2026, 9, 1))
                .checkOutDate(LocalDate.of(2026, 9, 5))
                .guests(2)
                .roomType(RoomType.DELUXE)
                .bookingStatus(BookingStatus.CONFIRMED)
                .totalRevenue(new BigDecimal("800.00"))
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = BookingCreateRequest.builder()
                .hotelName("Grand Hyatt")
                .guestName("John Doe")
                .checkInDate(LocalDate.of(2026, 9, 1))
                .checkOutDate(LocalDate.of(2026, 9, 5))
                .guests(2)
                .roomType(RoomType.DELUXE)
                .bookingStatus(BookingStatus.CONFIRMED)
                .totalRevenue(new BigDecimal("800.00"))
                .build();
    }

    @Test
    @DisplayName("Should successfully create a booking when input is valid")
    void createBooking_Success() {
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        BookingResponse response = bookingService.createBooking(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getHotelName()).isEqualTo("Grand Hyatt");
        assertThat(response.getTotalRevenue()).isEqualTo(new BigDecimal("800.00"));
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should throw InvalidBookingDatesException when checkOutDate is before checkInDate")
    void createBooking_InvalidDates_ThrowsException() {
        createRequest.setCheckOutDate(LocalDate.of(2026, 8, 30)); // Before check-in date Sept 1

        assertThatThrownBy(() -> bookingService.createBooking(createRequest))
                .isInstanceOf(InvalidBookingDatesException.class)
                .hasMessageContaining("must be strictly after check-in date");

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should return booking response when valid ID is provided")
    void getBookingById_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        BookingResponse response = bookingService.getBookingById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getGuestName()).isEqualTo("John Doe");
        verify(bookingRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when booking ID does not exist")
    void getBookingById_NotFound_ThrowsException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.getBookingById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Booking not found with ID: 99");
    }

    @Test
    @DisplayName("Should successfully update existing booking")
    void updateBooking_Success() {
        BookingUpdateRequest updateRequest = BookingUpdateRequest.builder()
                .hotelName("Grand Hyatt Updated")
                .guestName("John Doe")
                .checkInDate(LocalDate.of(2026, 9, 1))
                .checkOutDate(LocalDate.of(2026, 9, 6))
                .guests(2)
                .roomType(RoomType.SUITE)
                .bookingStatus(BookingStatus.CHECKED_IN)
                .totalRevenue(new BigDecimal("1000.00"))
                .build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        BookingResponse response = bookingService.updateBooking(1L, updateRequest);

        assertThat(response).isNotNull();
        verify(bookingRepository, times(1)).findById(1L);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should delete booking when ID exists")
    void deleteBooking_Success() {
        when(bookingRepository.existsById(1L)).thenReturn(true);
        doNothing().when(bookingRepository).deleteById(1L);

        bookingService.deleteBooking(1L);

        verify(bookingRepository, times(1)).existsById(1L);
        verify(bookingRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should return filtered bookings matching criteria")
    void getBookingsWithFilters_Success() {
        when(bookingRepository.findFilteredBookings("Grand", RoomType.DELUXE, BookingStatus.CONFIRMED, null, null))
                .thenReturn(List.of(sampleBooking));

        List<BookingResponse> responses = bookingService.getBookingsWithFilters("Grand", RoomType.DELUXE, BookingStatus.CONFIRMED, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getHotelName()).isEqualTo("Grand Hyatt");
        verify(bookingRepository, times(1)).findFilteredBookings("Grand", RoomType.DELUXE, BookingStatus.CONFIRMED, null, null);
    }

    @Test
    @DisplayName("Should return paginated bookings envelope")
    void getPagedBookings_Success() {
        org.springframework.data.domain.Page<Booking> page = new org.springframework.data.domain.PageImpl<>(
                List.of(sampleBooking), org.springframework.data.domain.PageRequest.of(0, 10), 1);
        when(bookingRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        com.mohana.staypulse.dto.response.PagedResponse<BookingResponse> response =
                bookingService.getPagedBookings(0, 10, "createdAt", "desc");

        assertThat(response).isNotNull();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1L);
        assertThat(response.getTotalPages()).isEqualTo(1);
        assertThat(response.isFirst()).isTrue();
        verify(bookingRepository, times(1)).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    @DisplayName("Should fall back to safe defaults for unknown sort field and oversized page request")
    void getPagedBookings_SanitizesUnsafeParameters() {
        org.springframework.data.domain.Page<Booking> page = new org.springframework.data.domain.PageImpl<>(
                List.of(sampleBooking), org.springframework.data.domain.PageRequest.of(0, 100), 1);
        when(bookingRepository.findAll(any(org.springframework.data.domain.Pageable.class))).thenReturn(page);

        bookingService.getPagedBookings(-5, 10_000, "notAField", "sideways");

        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        verify(bookingRepository).findAll(captor.capture());
        org.springframework.data.domain.Pageable used = captor.getValue();
        assertThat(used.getPageNumber()).isZero();
        assertThat(used.getPageSize()).isEqualTo(100);
        assertThat(used.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(used.getSort().getOrderFor("createdAt").isAscending()).isTrue();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating a missing booking")
    void updateBooking_NotFound_ThrowsException() {
        when(bookingRepository.findById(404L)).thenReturn(Optional.empty());
        BookingUpdateRequest request = BookingUpdateRequest.builder()
                .hotelName("Grand Hyatt").guestName("John Doe")
                .checkInDate(LocalDate.of(2026, 9, 1)).checkOutDate(LocalDate.of(2026, 9, 3))
                .guests(2).roomType(RoomType.DELUXE).bookingStatus(BookingStatus.CONFIRMED)
                .totalRevenue(new BigDecimal("400.00")).build();

        assertThatThrownBy(() -> bookingService.updateBooking(404L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should not call deleteById when the booking does not exist")
    void deleteBooking_NotFound_ThrowsException() {
        when(bookingRepository.existsById(77L)).thenReturn(false);

        assertThatThrownBy(() -> bookingService.deleteBooking(77L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("77");
        verify(bookingRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Should reject a same-day stay where check-out equals check-in")
    void createBooking_SameDayStay_ThrowsException() {
        createRequest.setCheckOutDate(createRequest.getCheckInDate());

        assertThatThrownBy(() -> bookingService.createBooking(createRequest))
                .isInstanceOf(InvalidBookingDatesException.class);
        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should trim hotel and guest names before persisting")
    void createBooking_TrimsNames() {
        createRequest.setHotelName("  Grand Hyatt  ");
        createRequest.setGuestName("  John Doe ");
        when(bookingRepository.save(any(Booking.class))).thenReturn(sampleBooking);

        bookingService.createBooking(createRequest);

        org.mockito.ArgumentCaptor<Booking> captor = org.mockito.ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getHotelName()).isEqualTo("Grand Hyatt");
        assertThat(captor.getValue().getGuestName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("Should return an empty list for a blank hotel lookup without querying")
    void getBookingsByHotel_Blank_ReturnsEmpty() {
        assertThat(bookingService.getBookingsByHotel("   ")).isEmpty();
        assertThat(bookingService.getBookingsByHotel(null)).isEmpty();
        verifyNoInteractions(bookingRepository);
    }
}
