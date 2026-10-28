package com.mohana.staypulse.dto.response;

import com.mohana.staypulse.entity.BookingStatus;
import com.mohana.staypulse.entity.RoomType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {

    private Long id;
    private String hotelName;
    private String guestName;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private Integer guests;
    private RoomType roomType;
    private BookingStatus bookingStatus;
    private BigDecimal totalRevenue;
    private LocalDateTime createdAt;
}
