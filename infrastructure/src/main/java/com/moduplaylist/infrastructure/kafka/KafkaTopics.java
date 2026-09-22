package com.moduplaylist.infrastructure.kafka;

public final class KafkaTopics {

    public static final String FOLLOW_CREATED = "follow.created";
    public static final String NOTIFICATION_CREATED = "notification.created";
    public static final String DM_SEND_REQUESTED = "dm.send.requested";
    public static final String DM_MESSAGE_CREATED = "dm.message.created";
    public static final String CONTENT_ACTIVITIES = "content-activities";
    public static final String CONTENT_LIFECYCLE = "content-lifecycle";
    public static final String WATCH_PARTY_PARTICIPANT_CHANGED = "watchparty.participant-changed";
    public static final String PLAYLIST_SUBSCRIBED = "playlist.subscribed";
    public static final String PLAYLIST_CONTENT_ADDED = "playlist.content-added";

    private KafkaTopics() {
    }
}
