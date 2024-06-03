package com.sabiasrl.poc.sb_integration_artemis;

import com.sabiasrl.poc.sb_integration_artemis.model.MessageFormatType;
import com.sabiasrl.poc.sb_integration_artemis.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.PollerSpec;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.jms.dsl.Jms;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.MessageChannel;

@SpringBootApplication
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(JmsTemplate jmsTemplate) {
        return args -> {
            var transaction = Transaction.builder()
                    .message("{eur:300.00}")
                    .receiverName("AAAABBCCDD")
                    .messageFormatType(MessageFormatType.MT1)
                    .build();

            jmsTemplate.convertAndSend("network", transaction);
            logger.info("Message sent {}", transaction);

            var receivedMessage = jmsTemplate.receiveAndConvert("AAAABBCCDD");
            logger.info("Received message: {}", receivedMessage);
        };
    }

    @Bean(name = PollerMetadata.DEFAULT_POLLER)
    public PollerSpec poller() {
        return Pollers.fixedDelay(1000);
    }

    /**
     * input gateway
     */
    @MessagingGateway
    public interface GatewayExample {
        @Gateway(requestChannel = "AAAABBCCDD")
        void branchNo1(Transaction transaction);

        @Gateway(requestChannel = "AAAABBCCEE")
        void branchNo2(Transaction transaction);
    }


    @Bean
    public MessageChannel network() {
        return new DirectChannel();
    }


    @Bean
    public IntegrationFlow jmsInboundFlow(JmsTemplate jmsTemplate, MessageChannel network) {
        return IntegrationFlow
                .from(Jms.inboundAdapter(jmsTemplate).destination("network"))
                .enrichHeaders(h ->
                        h.headerExpression("messageFormatType", "payload.messageFormatType.toString()")
                )
                .channel(network)
                .get();
    }

    /**
     * integration flow: transform message content it uppercase
     */
    @Bean
    public IntegrationFlow messagingHub(MessageChannel network, GatewayExample gateway) {
        return IntegrationFlow
                .from(network)
                .<Transaction, MessageFormatType>route(
                        Transaction::getMessageFormatType,
                        mapping -> mapping
                                .subFlowMapping(MessageFormatType.MT1, subFlow ->
                                        subFlow.transform(transaction ->
                                                        validatePayload((Transaction) transaction, MessageFormatType.MT1))
                                                .bridge()
                                )
                                .subFlowMapping(MessageFormatType.MT2, subFlow ->
                                        subFlow.transform(transaction ->
                                                        validatePayload((Transaction) transaction, MessageFormatType.MT2))
                                                .bridge()
                                )
                )
                .gateway(flow -> flow.handle((payload, headers) -> {
                    var transaction = (Transaction) payload;
                    switch (transaction.getReceiverName()) {
                        case "AAAABBCCDD": {
                            gateway.branchNo1(transaction);
                            break;
                        }
                        case "AAAABBCCEE": {
                            gateway.branchNo2(transaction);
                            break;
                        }
                        default: {
                            flow.channel("errorChannel");
                        }
                    }
                    return payload;
                }))
                .get();
    }

    private Transaction validatePayload(Transaction transaction, MessageFormatType messageFormatType) {
        var validationResult = switch (messageFormatType) {
            case MT1, MT2 -> true;
            default -> false;
        };
        if (!validationResult) {
            throw new RuntimeException("Message format not supported");
        }
        return transaction;
    }

    @Bean
    public IntegrationFlow jmsOutboundDestination1(JmsTemplate jmsTemplate) {
        return IntegrationFlow
                .from("AAAABBCCDD")
                .handle(Jms.outboundAdapter(jmsTemplate).destination("AAAABBCCDD"))
                .get();
    }

    @Bean
    public IntegrationFlow jmsOutboundDestination2(JmsTemplate jmsTemplate) {
        return IntegrationFlow
                .from("AAAABBCCEE")
                .handle(Jms.outboundAdapter(jmsTemplate).destination("AAAABBCCEE"))
                .get();
    }
}
