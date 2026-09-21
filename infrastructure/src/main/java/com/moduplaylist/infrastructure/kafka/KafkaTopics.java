package com.moduplaylist.infrastructure.kafka;

public final class KafkaTopics {

    public static final String FOLLOW_CREATED = "follow.created";
    public static final String NOTIFICATION_CREATED = "notification.created";
    public static final String DM_SEND_REQUESTED = "dm.send.requested";
    public static final String DM_MESSAGE_CREATED = "dm.message.created";
    public static final String CONTENT_ACTIVITIES = "content-activities";
    public static final String WATCH_PARTY_REMINDER_DUE = "watchparty.reminder-due";
    public static final String WATCH_PARTY_STATUS_CHANGED = "watchparty.status-changed";
    public static final String WATCH_PARTY_CREATED = "watchparty.created";
    public static final String WATCH_PARTY_PARTICIPANT_CHANGED = "watchparty.participant-changed";

    private KafkaTopics() {
    }
}
