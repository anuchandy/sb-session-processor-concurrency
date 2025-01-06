package com.anuchan.messaging.util;

import java.time.Duration;

public class Constants {
    public static final String SCENARIO_NAME = "scenario";
    public static final String AZURE_SERVICEBUS_CONNECTION_STRING = "AZURE_SERVICEBUS_CONNECTION_STRING";
    public static final String AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION = "AZURE_SERVICEBUS_TOPIC_SUBSCRIPTION";
    public static final String MAX_CONCURRENT_SESSIONS = "MAX_CONCURRENT_SESSIONS";
    public static final String MAX_CONCURRENT_CALLS = "MAX_CONCURRENT_CALLS";
    public static final String SESSION_IDLE_TIMEOUT_IN_SECONDS = "SESSION_IDLE_TIMEOUT_IN_SECONDS";
    public static final String SESSION_MAX_LOCK_RENEWAL_DURATION_IN_SECONDS = "SESSION_MAX_LOCK_RENEWAL_DURATION_IN_SECONDS";
    public static final String SEND_SESSIONS = "SEND_SESSIONS";
    public static final String SESSION_ID_MODE = "SESSION_ID_MODE"; // "SEQUENTIAL" or "RANDOM"
    public static final String SECONDS_IN_PERIOD = "SECONDS_IN_PERIOD";
    public static final String SEND_MESSAGES_PER_PERIOD = "SEND_MESSAGES_PER_PERIOD";
    public static final String SEND_CONCURRENCY = "SEND_CONCURRENCY";
    public static final Duration TEST_DURATION = Duration.ofHours(12);
}
