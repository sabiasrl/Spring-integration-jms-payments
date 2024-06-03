package com.sabiasrl.poc.sb_integration_artemis.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class Transaction implements Serializable {
    private final Object message;
    private final MessageFormatType messageFormatType;
    private final String receiverName;
}
