package com.anuchan.messaging.scenarios;

import com.anuchan.messaging.util.TopicSubscription;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusErrorContext;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.anuchan.messaging.util.Constants.TEST_DURATION;

@Service
public class ServiceBusProcessorScenario extends RunScenario {
    private final ClientLogger clientLogger = new ClientLogger(ServiceBusProcessorScenario.class);

    @Override
    public void run() {
        ServiceBusProcessorClient client = null;
        try {
            client = createProcessor();
            client.start();
            blockingWait(clientLogger, TEST_DURATION.plusSeconds(5));
        } finally {
            super.close(client);
        }
    }

    private ServiceBusProcessorClient createProcessor() {
        final String connectionString = super.getConnectionStringFromEnvironment();
        final TopicSubscription topicSubscription = super.getTopicSubscriptionFromEnvironment();
        final int maxConcurrentSessions = super.getMaxConcurrentSessionsFromEnvironment();
        final int maxConcurrentCalls = super.getMaxConcurrentCallsFromEnvironment();
        final Duration maxLockRenewalDuration = super.getSessionMaxLockRenewalDurationFromEnvironment();
        final Duration idleTimeout = super.getSessionIdleTimeoutFromEnvironment();

        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sessionProcessor()
                .topicName(topicSubscription.getTopicName())
                .subscriptionName(topicSubscription.getSubscriptionName())
                .disableAutoComplete()
                .maxAutoLockRenewDuration(maxLockRenewalDuration)
                .sessionIdleTimeout(idleTimeout)
                .maxConcurrentSessions(maxConcurrentSessions)
                .maxConcurrentCalls(maxConcurrentCalls)
                .prefetchCount(0)
                .processMessage(this::process)
                .processError(this::processError)
                .buildProcessorClient();
    }

    private void process(ServiceBusReceivedMessageContext messageContext) {
        settleMessage(messageContext);
    }

    private void settleMessage(ServiceBusReceivedMessageContext context) {
        try {
            context.complete();
            clientLogger.atInfo().log("Received and completed message: {} from session {}", context.getMessage().getMessageId(), context.getMessage().getSessionId());
        } catch (Throwable e) {
            clientLogger.atError().log("Error occurred while settling message.", e);
        }
    }

    private void processError(ServiceBusErrorContext errorContext) {
        clientLogger.atError().log("processError", errorContext.getException());
    }
}
