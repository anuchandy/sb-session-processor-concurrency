package com.anuchan.messaging.scenarios;

import com.anuchan.messaging.util.CmdlineArgs;
import com.anuchan.messaging.util.Constants;
import com.anuchan.messaging.util.SessionIdMode;
import com.anuchan.messaging.util.TopicSubscription;
import com.azure.core.util.CoreUtils;
import com.azure.core.util.logging.ClientLogger;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public abstract class RunScenario {
    @Autowired
    protected CmdlineArgs cmdlineArgs;

    @Autowired
    private ApplicationContext applicationContext;

    private void postConstruct() {
    }

    public abstract void run();

    protected boolean blockingWait(ClientLogger logger, Duration duration) {
        if (duration.toMillis() <= 0) {
            return true;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            logger.warning("wait interrupted");
            return false;
        }
        return true;
    }

    protected void close(List<ServiceBusProcessorClient> clients) {
        if (clients != null) {
            for (ServiceBusProcessorClient client : clients) {
                close(client);
            }
        }
    }

    protected boolean close(Disposable d) {
        if (d == null) {
            return true;
        }
        try {
            d.dispose();
        } catch (Exception e) {
            return false;
        }
        return true;

    }

    protected boolean close(AutoCloseable c) {
        if (c == null) {
            return true;
        }
        try {
            c.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    protected String getConnectionStringFromEnvironment() {
        final String connectionString = System.getenv(Constants.AZURE_SERVICEBUS_CONNECTION_STRING);
        if (CoreUtils.isNullOrEmpty(connectionString)) {
            throw new IllegalArgumentException("Environment variable 'AZURE_SERVICEBUS_CONNECTION_STRING' must be set.");
        }
        return connectionString;
    }

    protected List<TopicSubscription> getTopicSubscriptionsFromEnvironment() {
        final String topicSubscriptionEntries = System.getenv(Constants.AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION);
        return parse(topicSubscriptionEntries);
    }

    protected TopicSubscription getTopicSubscriptionFromEnvironment() {
        final List<TopicSubscription> topicSubscriptions = getTopicSubscriptionsFromEnvironment();
        if (topicSubscriptions.size() != 1) {
            throw new IllegalArgumentException("Exactly one topic subscription must be specified in AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION");
        }
        return topicSubscriptions.get(0);
    }

    protected int getMaxConcurrentSessionsFromEnvironment() {
        return getIntFromEnvironment(Constants.MAX_CONCURRENT_SESSIONS);
    }

    protected int getMaxConcurrentCallsFromEnvironment() {
        return getIntFromEnvironment(Constants.MAX_CONCURRENT_CALLS);
    }

    protected Duration getSessionIdleTimeoutFromEnvironment() {
        final int v = getIntFromEnvironment(Constants.SESSION_IDLE_TIMEOUT_IN_SECONDS);
        return Duration.ofSeconds(v);
    }

    protected Duration getSessionMaxLockRenewalDurationFromEnvironment() {
        int v = getIntFromEnvironment(Constants.SESSION_MAX_LOCK_RENEWAL_DURATION_IN_SECONDS);
        return Duration.ofSeconds(v);
    }

    protected int getSendSessionsFromEnvironment() {
        return getIntFromEnvironment(Constants.SEND_SESSIONS);
    }

    protected SessionIdMode getSessionIdModeFromEnvironment() {
        final String v = System.getenv(Constants.SESSION_ID_MODE);
        if ("SEQUENTIAL".equalsIgnoreCase(v)) {
            return SessionIdMode.SEQUENTIAL;
        } else if ("RANDOM".equalsIgnoreCase(v)) {
            return SessionIdMode.RANDOM;
        } else {
            throw new IllegalArgumentException("Environment variable 'SESSION_ID_MODE' must be set to either 'SEQUENTIAL' or 'RANDOM'.");
        }
    }

    protected int getSecondsInPeriodFromEnvironment() {
        return getIntFromEnvironment(Constants.SECONDS_IN_PERIOD);
    }

    protected int getSendMessagesPerPeriodFromEnvironment() {
        return getIntFromEnvironment(Constants.SEND_MESSAGES_PER_PERIOD);
    }

    protected int getSendConcurrencyFromEnvironment() {
        return getIntFromEnvironment(Constants.SEND_CONCURRENCY);
    }

    private int getIntFromEnvironment(String envName) {
        final String value = System.getenv(envName);
        if (CoreUtils.isNullOrEmpty(value)) {
            throw new IllegalArgumentException("Environment variable '" + envName + "' must be set.");
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Environment variable '" + envName + "' must be an integer.");
        }
    }

    private List<TopicSubscription> parse(String topicSubscriptionEntries) {
        if (CoreUtils.isNullOrEmpty(topicSubscriptionEntries)) {
            throw new IllegalArgumentException("AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION cannot be null or empty");
        }
        final List<TopicSubscription> topicSubscriptions = new ArrayList<>();
        String[] arr = topicSubscriptionEntries.split(";");
        for (String a : arr) {
            final String entry = a.trim();
            if (entry.isEmpty()) {
                continue;
            }
            String[] parts = entry.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid topic subscription entry in AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION: " + entry);
            }
            final String topicName = parts[0].trim();
            final String subscriptionName = parts[1].trim();
            if (topicName.isEmpty() || subscriptionName.isEmpty()) {
                throw new IllegalArgumentException("Invalid topic subscription entry in AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION: " + entry);
            }
            topicSubscriptions.add(new TopicSubscription(topicName, subscriptionName));
        }
        if (topicSubscriptions.isEmpty()) {
            throw new IllegalArgumentException("Invalid topic subscription entries AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION: " + topicSubscriptionEntries);
        }
        return topicSubscriptions;
    }
}
