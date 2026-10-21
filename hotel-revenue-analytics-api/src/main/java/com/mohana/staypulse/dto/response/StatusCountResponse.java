package com.mohana.staypulse.dto.response;

import com.mohana.staypulse.entity.BookingStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusCountResponse {

    private BookingStatus bookingStatus;
    private Long count;
}
