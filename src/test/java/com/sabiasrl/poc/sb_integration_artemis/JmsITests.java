package com.sabiasrl.poc.sb_integration_artemis;

import com.sabiasrl.poc.sb_integration_artemis.model.MessageFormatType;
import com.sabiasrl.poc.sb_integration_artemis.model.Transaction;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.support.MessageBuilder;

import java.time.Duration;

@SpringBootTest
class JmsITests {
    @Autowired
    private JmsTemplate jmsTemplate;

    @Test
    void jmsAdaptorsBranchNo1Test() {
        jmsTemplate.convertAndSend("jms.network", MessageBuilder.withPayload(
                        Transaction.builder()
                                .message("{eur:300.00}")
                                .receiverName("AAAABBCCDD")
                                .messageFormatType(MessageFormatType.MT1)
                                .build())
                .build());

        Awaitility
                .waitAtMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    var transaction = (Transaction) jmsTemplate.receiveAndConvert("AAAABBCCDD");
                    Assertions.assertNotNull(transaction);
                    Assertions.assertEquals("{eur:300.00}", transaction.getMessage());
                    Assertions.assertEquals(MessageFormatType.MT1, transaction.getMessageFormatType());
                });
    }
}