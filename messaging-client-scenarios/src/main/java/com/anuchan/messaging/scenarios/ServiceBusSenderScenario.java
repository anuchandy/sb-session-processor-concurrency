package com.anuchan.messaging.scenarios;

import com.anuchan.messaging.util.RateLimiter;
import com.anuchan.messaging.util.SessionIdMode;
import com.anuchan.messaging.util.TopicSubscription;
import com.azure.core.util.BinaryData;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderAsyncClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Random;

import static com.anuchan.messaging.util.Constants.TEST_DURATION;

@Service
public class ServiceBusSenderScenario extends RunScenario {
    private final ClientLogger clientLogger = new ClientLogger(ServiceBusSenderScenario.class);

    @Override
    public void run() {
        final int secondsInPeriod = super.getSecondsInPeriodFromEnvironment();
        final int messagesPerPeriod = super.getSendMessagesPerPeriodFromEnvironment();
        final int concurrency = super.getSendConcurrencyFromEnvironment();

        try(ServiceBusSenderAsyncClient client = createSender(); RateLimiter rateLimiter = createRateLimiter(secondsInPeriod, messagesPerPeriod, concurrency)) {
            final Flux<ServiceBusMessage> messages = messagesStream();
            messages.take(TEST_DURATION)
                    .flatMap(msg -> rateLimiter.acquire().then(client.sendMessage(msg).onErrorResume(t -> true, t -> {
                        clientLogger.atWarning().log("Error occurred while sending message.", t);
                        return Mono.empty();
                    }).doFinally(i -> rateLimiter.release())))
                    .parallel(concurrency, concurrency)
                    .runOn(Schedulers.boundedElastic())
                    .subscribe();
            blockingWait(clientLogger, TEST_DURATION.plusSeconds(5));
        }
    }

    private ServiceBusSenderAsyncClient createSender() {
        final String connectionString = super.getConnectionStringFromEnvironment();
        final TopicSubscription topicSubscription = super.getTopicSubscriptionFromEnvironment();
        return new ServiceBusClientBuilder()
                .connectionString(connectionString)
                .sender()
                .topicName(topicSubscription.getTopicName())
                .buildAsyncClient();
    }

    private RateLimiter createRateLimiter(int secondsInPeriod, int messagesPerPeriod, int concurrency) {
        return new RateLimiter(secondsInPeriod, messagesPerPeriod, concurrency);
    }

    private Flux<ServiceBusMessage> messagesStream() {
        final SessionIdMode mode = super.getSessionIdModeFromEnvironment();
        if (mode == SessionIdMode.SEQUENTIAL) {
            final int sendSessions = super.getSendSessionsFromEnvironment();
            final BinaryData payload = createMessagePayload(128);
            return Flux.range(0, sendSessions)
                    .repeat()
                    .map(id -> {
                        final String sessionId = id + "";
                        // clientLogger.atInfo().log("Sending message with sessionId: {}", sessionId);
                        return new ServiceBusMessage(payload).setSessionId(sessionId);
                    });
        } else {
            assert mode == SessionIdMode.RANDOM;
            final Random random = new Random();
            final int sendSessions = super.getSendSessionsFromEnvironment();
            final BinaryData payload = createMessagePayload(128);
            return Mono.fromSupplier(() -> {
                return random.nextInt(sendSessions);
            })
            .map(id -> {
                final String sessionId = id + "";
                // clientLogger.atInfo().log("Sending message with sessionId: {}", sessionId);
                return new ServiceBusMessage(payload).setSessionId(sessionId);
            })
            .repeat();
        }
    }

    private static BinaryData createMessagePayload(int messageSize) {
        final byte[] payload
                = "this is a circular payload that is used to fill up the message".getBytes(StandardCharsets.UTF_8);
        final StringBuilder body = new StringBuilder(messageSize);
        for (int i = 0; i < messageSize; i++) {
            body.append(payload[i % payload.length]);
        }
        return BinaryData.fromString(body.toString());
    }
}
