package com.mohana.staypulse.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopHotelResponse {

    private Integer rank;
    private String hotelName;
    private BigDecimal totalRevenue;
    private Long totalBookings;
}
