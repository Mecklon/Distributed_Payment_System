package com.mecklon.order.models;


import com.mecklon.order.models.types.SagaEventHistoryStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SagaEventHistory {
    private Instant time;
    private Boolean isCompensationEvent;
    private SagaEventHistoryStatus status;
}
