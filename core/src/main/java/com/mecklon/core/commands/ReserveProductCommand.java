package com.mecklon.core.commands;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReserveProductCommand {
    private String orderId;
    private List<ReserveProductCommandDetails> productList;
}
