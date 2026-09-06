package com.mohana.hotelanalytics.service;

import com.mohana.hotelanalytics.dto.response.*;
import com.mohana.hotelanalytics.entity.Booking;
import com.mohana.hotelanalytics.entity.BookingStatus;
import com.mohana.hotelanalytics.repository.BookingRepository;
import com.mohana.hotelanalytics.service.impl.AnalyticsServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    @DisplayName("Should return total revenue and active booking count")
    void getTotalRevenue_Success() {
        when(bookingRepository.findTotalRevenueOfActiveBookings()).thenReturn(new BigDecimal("5000.00"));
        when(bookingRepository.countActiveBookings()).thenReturn(10L);

        TotalRevenueResponse response = analyticsService.getTotalRevenue();

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualTo(new BigDecimal("5000.00"));
        assertThat(response.getEligibleBookingsCount()).isEqualTo(10L);
    }

    @Test
    @DisplayName("Should return revenue breakdown grouped by hotel")
    void getRevenueByHotel_Success() {
        Object[] hotelRow = new Object[]{"Grand Hyatt", new BigDecimal("3000.00"), 5L};
        List<Object[]> rows = Collections.singletonList(hotelRow);
        when(bookingRepository.findRevenueGroupedByHotel()).thenReturn(rows);

        List<HotelRevenueResponse> response = analyticsService.getRevenueByHotel();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getHotelName()).isEqualTo("Grand Hyatt");
        assertThat(response.get(0).getTotalRevenue()).isEqualTo(new BigDecimal("3000.00"));
        assertThat(response.get(0).getBookingCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("Should return monthly revenue trends with period growth rate")
    void getRevenueByMonth_Success() {
        Object[] month1 = new Object[]{"2026-08", new BigDecimal("2000.00"), 3L};
        Object[] month2 = new Object[]{"2026-09", new BigDecimal("2500.00"), 4L};
        List<Object[]> rows = List.of(month1, month2);
        when(bookingRepository.findMonthlyRevenueTrend()).thenReturn(rows);

        List<MonthlyRevenueResponse> response = analyticsService.getRevenueByMonth();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getYearMonth()).isEqualTo("2026-08");
        assertThat(response.get(0).getGrowthRatePercentage()).isNull();

        assertThat(response.get(1).getYearMonth()).isEqualTo("2026-09");
        assertThat(response.get(1).getTotalRevenue()).isEqualTo(new BigDecimal("2500.00"));
        assertThat(response.get(1).getPreviousMonthRevenue()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(response.get(1).getGrowthRatePercentage()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Should return booking count grouped by status")
    void getBookingCountByStatus_Success() {
        Object[] statusRow = new Object[]{BookingStatus.CONFIRMED, 8L};
        List<Object[]> rows = Collections.singletonList(statusRow);
        when(bookingRepository.findBookingCountGroupedByStatus()).thenReturn(rows);

        List<StatusCountResponse> response = analyticsService.getBookingCountByStatus();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getBookingStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.get(0).getCount()).isEqualTo(8L);
    }

    @Test
    @DisplayName("Should return average revenue per booking")
    void getAverageRevenue_Success() {
        when(bookingRepository.findAverageRevenuePerBooking()).thenReturn(450.75);
        when(bookingRepository.countActiveBookings()).thenReturn(8L);

        AverageRevenueResponse response = analyticsService.getAverageRevenue();

        assertThat(response).isNotNull();
        assertThat(response.getAverageRevenuePerBooking()).isEqualTo(new BigDecimal("450.75"));
        assertThat(response.getTotalBookingsEvaluated()).isEqualTo(8L);
    }

    @Test
    @DisplayName("Should return revenue breakdown grouped by room type")
    void getRevenueByRoomType_Success() {
        Object[] suiteRow = new Object[]{com.mohana.hotelanalytics.entity.RoomType.SUITE, new BigDecimal("4000.00"), 4L};
        List<Object[]> rows = Collections.singletonList(suiteRow);
        when(bookingRepository.findRevenueGroupedByRoomType()).thenReturn(rows);
        when(bookingRepository.findTotalRevenueOfActiveBookings()).thenReturn(new BigDecimal("8000.00"));

        List<RoomTypeRevenueResponse> response = analyticsService.getRevenueByRoomType();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRoomType()).isEqualTo(com.mohana.hotelanalytics.entity.RoomType.SUITE);
        assertThat(response.get(0).getTotalRevenue()).isEqualTo(new BigDecimal("4000.00"));
        assertThat(response.get(0).getBookingCount()).isEqualTo(4L);
        assertThat(response.get(0).getAverageRevenue()).isEqualTo(new BigDecimal("1000.00"));
        assertThat(response.get(0).getRevenuePercentage()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should return operational KPI metrics including ADR and ALOS")
    void getOperationalMetrics_Success() {
        Booking b1 = Booking.builder()
                .id(1L)
                .bookingStatus(BookingStatus.CONFIRMED)
                .checkInDate(java.time.LocalDate.of(2026, 9, 1))
                .checkOutDate(java.time.LocalDate.of(2026, 9, 4)) // 3 nights
                .totalRevenue(new BigDecimal("600.00"))
                .build();

        Booking b2 = Booking.builder()
                .id(2L)
                .bookingStatus(BookingStatus.CANCELLED)
                .checkInDate(java.time.LocalDate.of(2026, 9, 2))
                .checkOutDate(java.time.LocalDate.of(2026, 9, 5))
                .totalRevenue(new BigDecimal("500.00"))
                .build();

        when(bookingRepository.findAll()).thenReturn(List.of(b1, b2));

        OperationalMetricsResponse response = analyticsService.getOperationalMetrics();

        assertThat(response).isNotNull();
        assertThat(response.getTotalBookings()).isEqualTo(2L);
        assertThat(response.getActiveBookings()).isEqualTo(1L);
        assertThat(response.getCancelledBookings()).isEqualTo(1L);
        assertThat(response.getTotalRoomNights()).isEqualTo(3L);
        assertThat(response.getAverageDailyRate()).isEqualTo(new BigDecimal("200.00"));
        assertThat(response.getAverageLengthOfStay()).isEqualTo(3.0);
        assertThat(response.getCancellationRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should return top hotels by revenue limit")
    void getTopHotels_Success() {
        Object[] topHotelRow = new Object[]{"Ritz Carlton", new BigDecimal("4500.00"), 6L};
        List<Object[]> rows = Collections.singletonList(topHotelRow);
        when(bookingRepository.findTopHotelsByRevenue(PageRequest.of(0, 3))).thenReturn(rows);

        List<TopHotelResponse> response = analyticsService.getTopHotels(3);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRank()).isEqualTo(1);
        assertThat(response.get(0).getHotelName()).isEqualTo("Ritz Carlton");
    }

    @Test
    @DisplayName("Should export bookings into valid CSV byte array")
    void exportBookingsCsv_Success() {
        Booking sampleBooking = Booking.builder()
                .id(1L)
                .hotelName("Grand Horizon Resort")
                .guestName("Alice Walker")
                .checkInDate(java.time.LocalDate.of(2026, 9, 1))
                .checkOutDate(java.time.LocalDate.of(2026, 9, 5))
                .guests(2)
                .roomType(com.mohana.hotelanalytics.entity.RoomType.DELUXE)
                .bookingStatus(com.mohana.hotelanalytics.entity.BookingStatus.CONFIRMED)
                .totalRevenue(new BigDecimal("1200.00"))
                .createdAt(java.time.LocalDateTime.now())
                .build();

        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(sampleBooking));

        byte[] csvBytes = analyticsService.exportBookingsCsv();
        String csvContent = new String(csvBytes, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csvContent).contains("ID,Hotel Name,Guest Name");
        assertThat(csvContent).contains("Grand Horizon Resort");
        assertThat(csvContent).contains("Alice Walker");
    }
}
