package com.moduplaylist.realtime.kafka;

public final class KafkaTopics {

    public static final String NOTIFICATION_CREATED =
            "notification.created";
    public static final String DM_SEND_REQUESTED =
            "dm.send.requested";
    public static final String DM_MESSAGE_CREATED =
            "dm.message.created";

    private KafkaTopics() {
    }
}
