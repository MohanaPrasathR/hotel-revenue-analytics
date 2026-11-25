package com.mohana.staypulse.service;

import com.mohana.staypulse.dto.response.*;
import com.mohana.staypulse.entity.Booking;
import com.mohana.staypulse.entity.BookingStatus;
import com.mohana.staypulse.repository.BookingRepository;
import com.mohana.staypulse.service.impl.AnalyticsServiceImpl;
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
        Object[] suiteRow = new Object[]{com.mohana.staypulse.entity.RoomType.SUITE, new BigDecimal("4000.00"), 4L};
        List<Object[]> rows = Collections.singletonList(suiteRow);
        when(bookingRepository.findRevenueGroupedByRoomType()).thenReturn(rows);
        when(bookingRepository.findTotalRevenueOfActiveBookings()).thenReturn(new BigDecimal("8000.00"));

        List<RoomTypeRevenueResponse> response = analyticsService.getRevenueByRoomType();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getRoomType()).isEqualTo(com.mohana.staypulse.entity.RoomType.SUITE);
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
    @DisplayName("Should cap oversized top-hotel limit and default non-positive limits")
    void getTopHotels_CapsAndDefaultsLimit() {
        when(bookingRepository.findTopHotelsByRevenue(PageRequest.of(0, 50))).thenReturn(Collections.emptyList());
        when(bookingRepository.findTopHotelsByRevenue(PageRequest.of(0, 5))).thenReturn(Collections.emptyList());

        assertThat(analyticsService.getTopHotels(100000)).isEmpty();
        assertThat(analyticsService.getTopHotels(-1)).isEmpty();

        org.mockito.Mockito.verify(bookingRepository).findTopHotelsByRevenue(PageRequest.of(0, 50));
        org.mockito.Mockito.verify(bookingRepository).findTopHotelsByRevenue(PageRequest.of(0, 5));
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
                .roomType(com.mohana.staypulse.entity.RoomType.DELUXE)
                .bookingStatus(com.mohana.staypulse.entity.BookingStatus.CONFIRMED)
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

    @Test
    @DisplayName("Should neutralise spreadsheet formulas and escape quotes in exported CSV")
    void exportBookingsCsv_NeutralisesFormulaInjection() {
        Booking risky = Booking.builder()
                .id(2L)
                .hotelName("=HYPERLINK(\"http://evil.test\")")
                .guestName("@SUM(A1)")
                .checkInDate(java.time.LocalDate.of(2026, 9, 1))
                .checkOutDate(java.time.LocalDate.of(2026, 9, 2))
                .guests(1)
                .roomType(com.mohana.staypulse.entity.RoomType.DELUXE)
                .bookingStatus(com.mohana.staypulse.entity.BookingStatus.CONFIRMED)
                .totalRevenue(new BigDecimal("100.00"))
                .createdAt(java.time.LocalDateTime.now())
                .build();
        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(risky));

        String csv = new String(analyticsService.exportBookingsCsv(), java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv).contains("\"'=HYPERLINK(\"\"http://evil.test\"\")\"");
        assertThat(csv).contains("\"'@SUM(A1)\"");
    }

    @Test
    @DisplayName("Should generate accurate revenue summary digest")
    void getRevenueSummaryDigest_Success() {
        when(bookingRepository.findTotalRevenueOfActiveBookings()).thenReturn(new BigDecimal("12000.00"));
        when(bookingRepository.countActiveBookings()).thenReturn(15L);
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());
        when(bookingRepository.findTopHotelsByRevenue(PageRequest.of(0, 1)))
                .thenReturn(Collections.singletonList(new Object[]{"Grand Horizon", new BigDecimal("7000.00"), 8L}));
        when(bookingRepository.findRevenueGroupedByRoomType())
                .thenReturn(Collections.singletonList(new Object[]{com.mohana.staypulse.entity.RoomType.DELUXE, new BigDecimal("6000.00"), 6L}));

        RevenueSummaryDigestResponse response = analyticsService.getRevenueSummaryDigest();

        assertThat(response).isNotNull();
        assertThat(response.getTotalRevenue()).isEqualTo(new BigDecimal("12000.00"));
        assertThat(response.getTopPerformingHotel()).isEqualTo("Grand Horizon");
        assertThat(response.getTopHotelRevenue()).isEqualTo(new BigDecimal("7000.00"));
        assertThat(response.getMostPopularRoomType()).isEqualTo("DELUXE");
    }

    @Test
    @DisplayName("Should compute month-over-month growth and leave the first month without growth")
    void getRevenueByMonth_ComputesGrowth() {
        List<Object[]> rows = List.of(
                new Object[]{"2026-08", new BigDecimal("1000.00"), 2L},
                new Object[]{"2026-09", new BigDecimal("1500.00"), 3L});
        when(bookingRepository.findMonthlyRevenueTrend()).thenReturn(rows);

        List<MonthlyRevenueResponse> response = analyticsService.getRevenueByMonth();

        assertThat(response).hasSize(2);
        assertThat(response.get(0).getGrowthRatePercentage()).isNull();
        assertThat(response.get(1).getGrowthRatePercentage()).isEqualTo(50.0);
        assertThat(response.get(1).getPreviousMonthRevenue()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    @DisplayName("Should not report growth when the previous month had zero revenue")
    void getRevenueByMonth_ZeroPreviousMonth_NoGrowth() {
        List<Object[]> rows = List.of(
                new Object[]{"2026-08", BigDecimal.ZERO, 0L},
                new Object[]{"2026-09", new BigDecimal("900.00"), 2L});
        when(bookingRepository.findMonthlyRevenueTrend()).thenReturn(rows);

        List<MonthlyRevenueResponse> response = analyticsService.getRevenueByMonth();

        assertThat(response.get(1).getGrowthRatePercentage()).isNull();
    }

    @Test
    @DisplayName("Should compute room type average revenue and share of total revenue")
    void getRevenueByRoomType_ComputesShareAndAverage() {
        when(bookingRepository.findRevenueGroupedByRoomType()).thenReturn(Collections.singletonList(
                new Object[]{com.mohana.staypulse.entity.RoomType.DELUXE, new BigDecimal("250.00"), 2L}));
        when(bookingRepository.findTotalRevenueOfActiveBookings()).thenReturn(new BigDecimal("1000.00"));

        List<RoomTypeRevenueResponse> response = analyticsService.getRevenueByRoomType();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getAverageRevenue()).isEqualByComparingTo("125.00");
        assertThat(response.get(0).getRevenuePercentage()).isEqualTo(25.0);
    }

    @Test
    @DisplayName("Should calculate ADR, ALOS and cancellation rate from bookings")
    void getOperationalMetrics_ComputesKpis() {
        Booking active = Booking.builder().id(1L).hotelName("A").guestName("G1")
                .checkInDate(java.time.LocalDate.of(2026, 9, 1)).checkOutDate(java.time.LocalDate.of(2026, 9, 5))
                .guests(2).roomType(com.mohana.staypulse.entity.RoomType.DELUXE)
                .bookingStatus(BookingStatus.CONFIRMED).totalRevenue(new BigDecimal("800.00")).build();
        Booking cancelled = Booking.builder().id(2L).hotelName("A").guestName("G2")
                .checkInDate(java.time.LocalDate.of(2026, 9, 2)).checkOutDate(java.time.LocalDate.of(2026, 9, 4))
                .guests(1).roomType(com.mohana.staypulse.entity.RoomType.SINGLE)
                .bookingStatus(BookingStatus.CANCELLED).totalRevenue(new BigDecimal("300.00")).build();
        when(bookingRepository.findAll()).thenReturn(List.of(active, cancelled));

        OperationalMetricsResponse response = analyticsService.getOperationalMetrics();

        assertThat(response.getTotalRoomNights()).isEqualTo(4L);
        assertThat(response.getAverageDailyRate()).isEqualByComparingTo("200.00");
        assertThat(response.getAverageLengthOfStay()).isEqualTo(4.0);
        assertThat(response.getCancellationRate()).isEqualTo(50.0);
        assertThat(response.getTotalBookings()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Should return zeroed operational metrics when there are no bookings")
    void getOperationalMetrics_Empty_ReturnsZeros() {
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        OperationalMetricsResponse response = analyticsService.getOperationalMetrics();

        assertThat(response.getTotalBookings()).isZero();
        assertThat(response.getAverageDailyRate()).isEqualByComparingTo("0");
        assertThat(response.getCancellationRate()).isZero();
    }

    @Test
    @DisplayName("Should fall back to zero when no active bookings exist for revenue and average")
    void totals_NullAggregates_FallBackToZero() {
        when(bookingRepository.findTotalRevenueOfActiveBookings()).thenReturn(null);
        when(bookingRepository.countActiveBookings()).thenReturn(null);
        when(bookingRepository.findAverageRevenuePerBooking()).thenReturn(null);

        assertThat(analyticsService.getTotalRevenue().getTotalRevenue()).isEqualByComparingTo("0");
        assertThat(analyticsService.getTotalRevenue().getEligibleBookingsCount()).isZero();
        assertThat(analyticsService.getAverageRevenue().getAverageRevenuePerBooking()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Should return zero room type percentages when there is no revenue")
    void getRevenueByRoomType_NoRevenue_ZeroPercentage() {
        when(bookingRepository.findRevenueGroupedByRoomType()).thenReturn(Collections.singletonList(
                new Object[]{com.mohana.staypulse.entity.RoomType.SINGLE, BigDecimal.ZERO, 1L}));
        when(bookingRepository.findTotalRevenueOfActiveBookings()).thenReturn(null);

        List<RoomTypeRevenueResponse> response = analyticsService.getRevenueByRoomType();

        assertThat(response.get(0).getRevenuePercentage()).isZero();
    }

    @Test
    @DisplayName("Should assign sequential ranks to top hotels in repository order")
    void getTopHotels_AssignsSequentialRanks() {
        List<Object[]> rows = List.of(
                new Object[]{"Alpha", new BigDecimal("900.00"), 3L},
                new Object[]{"Beta", new BigDecimal("500.00"), 2L});
        when(bookingRepository.findTopHotelsByRevenue(PageRequest.of(0, 2))).thenReturn(rows);

        List<TopHotelResponse> response = analyticsService.getTopHotels(2);

        assertThat(response).extracting(TopHotelResponse::getRank).containsExactly(1, 2);
        assertThat(response).extracting(TopHotelResponse::getHotelName).containsExactly("Alpha", "Beta");
    }

    @Test
    @DisplayName("Should map booking status counts including cancelled bookings")
    void getBookingCountByStatus_MapsRows() {
        List<Object[]> rows = List.of(
                new Object[]{BookingStatus.CONFIRMED, 7L},
                new Object[]{BookingStatus.CANCELLED, 2L});
        when(bookingRepository.findBookingCountGroupedByStatus()).thenReturn(rows);

        List<StatusCountResponse> response = analyticsService.getBookingCountByStatus();

        assertThat(response).hasSize(2);
        assertThat(response.get(1).getBookingStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.get(1).getCount()).isEqualTo(2L);
    }
}
