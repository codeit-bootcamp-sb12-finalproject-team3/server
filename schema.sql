-- MySQL 8.0 기준
SET NAMES utf8mb4;


-- =================================================================
-- 사용자

CREATE TABLE users (
                       id                  BINARY(16) NOT NULL,
                       email               VARCHAR(100) NOT NULL,
                       password            VARCHAR(100) NULL,
                       name                VARCHAR(100) NOT NULL,
                       profile_image_url   VARCHAR(500) NULL,
                       role                ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
                       locked              BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE users
    ADD CONSTRAINT pk_users PRIMARY KEY (id);

ALTER TABLE users
    ADD CONSTRAINT uq_users_email UNIQUE (email);


-- =================================================================
-- OAuth 계정

CREATE TABLE user_oauth_accounts (
                                     id                  BINARY(16) NOT NULL,
                                     user_id             BINARY(16) NOT NULL,
                                     provider            ENUM('GOOGLE', 'KAKAO') NOT NULL,
                                     provider_user_id    VARCHAR(255) NOT NULL,
                                     created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE user_oauth_accounts
    ADD CONSTRAINT pk_user_oauth_accounts PRIMARY KEY (id);

ALTER TABLE user_oauth_accounts
    ADD CONSTRAINT uq_user_oauth_provider_user
        UNIQUE (provider, provider_user_id);

ALTER TABLE user_oauth_accounts
    ADD CONSTRAINT uq_user_oauth_user_provider
        UNIQUE (user_id, provider);

ALTER TABLE user_oauth_accounts
    ADD CONSTRAINT fk_user_oauth_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;


-- =================================================================
-- 콘텐츠

CREATE TABLE contents (
                          id                  BINARY(16) NOT NULL,
                          parent_content_id   BINARY(16) NULL,
                          title               VARCHAR(255) NOT NULL,
                          season_number       INT NULL,
                          season_count        INT NULL,
                          episode_count       INT NULL,
                          type                ENUM('movie', 'tvSeries', 'tvSeason', 'sport') NOT NULL,
                          sport_type          VARCHAR(50) NULL,
                          description         TEXT NULL,
                          thumbnail_url       VARCHAR(500) NULL,
                          release_date        DATE NULL,
                          runtime             INT NULL,
                          metadata            JSON NULL,
                          external_source     VARCHAR(30) NULL,
                          external_id         INT NULL,
                          average_rating      DECIMAL(3,2) NOT NULL DEFAULT 0.00,
                          like_count          INT UNSIGNED NOT NULL DEFAULT 0,
                          review_count        INT UNSIGNED NOT NULL DEFAULT 0,
                          created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE contents
    ADD CONSTRAINT pk_contents PRIMARY KEY (id);

ALTER TABLE contents
    ADD CONSTRAINT uq_contents_external
        UNIQUE (external_source, type, external_id);

ALTER TABLE contents
    ADD CONSTRAINT uq_contents_parent_season
        UNIQUE (parent_content_id, season_number);

ALTER TABLE contents
    ADD CONSTRAINT fk_contents_parent
        FOREIGN KEY (parent_content_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_external_pair
        CHECK (
            (external_source IS NULL AND external_id IS NULL)
                OR
            (external_source IS NOT NULL AND external_id IS NOT NULL)
            );

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_average_rating
        CHECK (average_rating BETWEEN 0.00 AND 5.00);

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_runtime
        CHECK (runtime IS NULL OR runtime > 0);

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_season_number
        CHECK (season_number IS NULL OR season_number >= 0);

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_season_count
        CHECK (season_count IS NULL OR season_count >= 0);

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_episode_count
        CHECK (episode_count IS NULL OR episode_count >= 0);

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_tv_season
        CHECK (
            (
                type = 'tvSeason'
                    AND parent_content_id IS NOT NULL
                    AND season_number IS NOT NULL
                )
                OR
            (
                type <> 'tvSeason'
                    AND parent_content_id IS NULL
                    AND season_number IS NULL
                    AND episode_count IS NULL
                )
            );

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_sport_type
        CHECK (
            (type = 'sport' AND sport_type IS NOT NULL)
                OR
            (type <> 'sport' AND sport_type IS NULL)
            );

ALTER TABLE contents
    ADD CONSTRAINT chk_contents_series_count
        CHECK (
            (type = 'tvSeries')
                OR
            (season_count IS NULL)
            );

CREATE INDEX idx_contents_created
    ON contents (created_at DESC, id DESC);

CREATE INDEX idx_contents_rating
    ON contents (average_rating DESC, id DESC);

CREATE INDEX idx_contents_type_created
    ON contents (type, created_at DESC, id DESC);

CREATE INDEX idx_contents_type_rating
    ON contents (type, average_rating DESC, id DESC);

CREATE INDEX idx_contents_sport_created
    ON contents (sport_type, created_at DESC, id DESC);


-- =================================================================
-- TV 시즌 회차

CREATE TABLE episodes (
                          id                  BINARY(16) NOT NULL,
                          season_id           BINARY(16) NOT NULL,
                          episode_number      INT NOT NULL,
                          title               VARCHAR(255) NOT NULL,
                          description         TEXT NULL,
                          still_image_url     VARCHAR(500) NULL,
                          runtime             INT NULL,
                          air_date            DATE NULL,
                          external_id         INT NOT NULL,
                          created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                          updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE episodes
    ADD CONSTRAINT pk_episodes PRIMARY KEY (id);

ALTER TABLE episodes
    ADD CONSTRAINT uq_episodes_external_id
        UNIQUE (external_id);

ALTER TABLE episodes
    ADD CONSTRAINT uq_episodes_season_number
        UNIQUE (season_id, episode_number);

ALTER TABLE episodes
    ADD CONSTRAINT fk_episodes_season
        FOREIGN KEY (season_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

ALTER TABLE episodes
    ADD CONSTRAINT chk_episodes_number
        CHECK (episode_number >= 0);

ALTER TABLE episodes
    ADD CONSTRAINT chk_episodes_runtime
        CHECK (runtime IS NULL OR runtime > 0);


-- =================================================================
-- 장르

CREATE TABLE genres (
                        id                  BINARY(16) NOT NULL,
                        name                VARCHAR(50) NOT NULL,
                        external_source     VARCHAR(30) NOT NULL,
                        external_id         INT NOT NULL
);

ALTER TABLE genres
    ADD CONSTRAINT pk_genres PRIMARY KEY (id);

ALTER TABLE genres
    ADD CONSTRAINT uq_genres_name UNIQUE (name);

ALTER TABLE genres
    ADD CONSTRAINT uq_genres_external
        UNIQUE (external_source, external_id);


-- =================================================================
-- 콘텐츠-장르 관계

CREATE TABLE content_genres (
                                id                  BINARY(16) NOT NULL,
                                content_id          BINARY(16) NOT NULL,
                                genre_id            BINARY(16) NOT NULL
);

ALTER TABLE content_genres
    ADD CONSTRAINT pk_content_genres PRIMARY KEY (id);

ALTER TABLE content_genres
    ADD CONSTRAINT uq_content_genres
        UNIQUE (content_id, genre_id);

ALTER TABLE content_genres
    ADD CONSTRAINT fk_content_genres_content
        FOREIGN KEY (content_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

ALTER TABLE content_genres
    ADD CONSTRAINT fk_content_genres_genre
        FOREIGN KEY (genre_id)
            REFERENCES genres (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_content_genres_genre
    ON content_genres (genre_id);


-- =================================================================
-- 태그

CREATE TABLE tags (
                      id                  BINARY(16) NOT NULL,
                      name                VARCHAR(100) NOT NULL
);

ALTER TABLE tags
    ADD CONSTRAINT pk_tags PRIMARY KEY (id);

ALTER TABLE tags
    ADD CONSTRAINT uq_tags_name UNIQUE (name);


-- =================================================================
-- 콘텐츠-태그 관계

CREATE TABLE content_tags (
                              id                  BINARY(16) NOT NULL,
                              content_id          BINARY(16) NOT NULL,
                              tag_id              BINARY(16) NOT NULL,
                              source              ENUM('MANUAL', 'AI', 'EXTERNAL') NOT NULL
);

ALTER TABLE content_tags
    ADD CONSTRAINT pk_content_tags PRIMARY KEY (id);

ALTER TABLE content_tags
    ADD CONSTRAINT uq_content_tags
        UNIQUE (content_id, tag_id);

ALTER TABLE content_tags
    ADD CONSTRAINT fk_content_tags_content
        FOREIGN KEY (content_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

ALTER TABLE content_tags
    ADD CONSTRAINT fk_content_tags_tag
        FOREIGN KEY (tag_id)
            REFERENCES tags (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_content_tags_content_source
    ON content_tags (content_id, source);

CREATE INDEX idx_content_tags_tag
    ON content_tags (tag_id);


-- =================================================================
-- 사용자 콘텐츠 태그 선호도
CREATE TABLE user_content_tag_preferences (
                                              id                  BINARY(16) NOT NULL,
                                              user_id             BINARY(16) NOT NULL,
                                              tag_id              BINARY(16) NOT NULL,
                                              score               DOUBLE NOT NULL DEFAULT 0,
                                              updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE user_content_tag_preferences
    ADD CONSTRAINT pk_user_content_tag_preferences
        PRIMARY KEY (id);

ALTER TABLE user_content_tag_preferences
    ADD CONSTRAINT uq_user_content_tag_preferences
        UNIQUE (user_id, tag_id);

ALTER TABLE user_content_tag_preferences
    ADD CONSTRAINT fk_user_content_tag_preferences_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE user_content_tag_preferences
    ADD CONSTRAINT fk_user_content_tag_preferences_tag
        FOREIGN KEY (tag_id)
            REFERENCES tags (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_user_content_tag_preferences_tag
    ON user_content_tag_preferences (tag_id);


-- =================================================================
-- 출연진

CREATE TABLE content_casts (
                               id                  BINARY(16) NOT NULL,
                               content_id          BINARY(16) NOT NULL,
                               name                VARCHAR(100) NOT NULL,
                               display_order       INT NOT NULL,
                               role_name           VARCHAR(255) NULL,
                               profile_image_url   VARCHAR(500) NULL
);

ALTER TABLE content_casts
    ADD CONSTRAINT pk_content_casts PRIMARY KEY (id);

ALTER TABLE content_casts
    ADD CONSTRAINT uq_content_casts_order
        UNIQUE (content_id, display_order);

ALTER TABLE content_casts
    ADD CONSTRAINT fk_content_casts_content
        FOREIGN KEY (content_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

ALTER TABLE content_casts
    ADD CONSTRAINT chk_content_casts_order
        CHECK (display_order >= 0);


-- =================================================================
-- OTT 플랫폼

CREATE TABLE ott_platforms (
                               id                  BINARY(16) NOT NULL,
                               name                VARCHAR(50) NOT NULL,
                               logo_url            VARCHAR(500) NULL,
                               created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE ott_platforms
    ADD CONSTRAINT pk_ott_platforms PRIMARY KEY (id);

ALTER TABLE ott_platforms
    ADD CONSTRAINT uq_ott_platforms_name UNIQUE (name);


-- =================================================================
-- 콘텐츠-OTT 관계

CREATE TABLE content_ott (
                             id                  BINARY(16) NOT NULL,
                             content_id          BINARY(16) NOT NULL,
                             ott_id              BINARY(16) NOT NULL,
                             watch_url           VARCHAR(1000) NULL,
                             updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE content_ott
    ADD CONSTRAINT pk_content_ott PRIMARY KEY (id);

ALTER TABLE content_ott
    ADD CONSTRAINT uq_content_ott
        UNIQUE (content_id, ott_id);

ALTER TABLE content_ott
    ADD CONSTRAINT fk_content_ott_content
        FOREIGN KEY (content_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

ALTER TABLE content_ott
    ADD CONSTRAINT fk_content_ott_platform
        FOREIGN KEY (ott_id)
            REFERENCES ott_platforms (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_content_ott_platform
    ON content_ott (ott_id);


-- =================================================================
-- 사용자 초기 선호 콘텐츠

CREATE TABLE user_preference_contents (
                                          id                  BINARY(16) NOT NULL,
                                          user_id             BINARY(16) NOT NULL,
                                          content_id          BINARY(16) NOT NULL,
                                          created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE user_preference_contents
    ADD CONSTRAINT pk_user_preference_contents PRIMARY KEY (id);

ALTER TABLE user_preference_contents
    ADD CONSTRAINT uq_user_preference_contents
        UNIQUE (user_id, content_id);

ALTER TABLE user_preference_contents
    ADD CONSTRAINT fk_user_preference_contents_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE user_preference_contents
    ADD CONSTRAINT fk_user_preference_contents_content
        FOREIGN KEY (content_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

CREATE INDEX idx_user_preference_contents_content
    ON user_preference_contents (content_id);


-- =================================================================
-- 콘텐츠 좋아요

CREATE TABLE content_likes (
                               id                  BINARY(16) NOT NULL,
                               user_id             BINARY(16) NOT NULL,
                               content_id          BINARY(16) NOT NULL,
                               created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE content_likes
    ADD CONSTRAINT pk_content_likes PRIMARY KEY (id);

ALTER TABLE content_likes
    ADD CONSTRAINT uq_content_likes
        UNIQUE (user_id, content_id);

ALTER TABLE content_likes
    ADD CONSTRAINT fk_content_likes_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE content_likes
    ADD CONSTRAINT fk_content_likes_content
        FOREIGN KEY (content_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

CREATE INDEX idx_content_likes_content
    ON content_likes (content_id);

CREATE INDEX idx_content_likes_user_created
    ON content_likes (user_id, created_at DESC, content_id);


-- =================================================================
-- 리뷰 및 평점

CREATE TABLE reviews (
                         id                  BINARY(16) NOT NULL,
                         user_id             BINARY(16) NOT NULL,
                         content_id          BINARY(16) NOT NULL,
                         content             TEXT NOT NULL,
                         rating              DECIMAL(2,1) NOT NULL,
                         is_spoiler          BOOLEAN NOT NULL DEFAULT FALSE,
                         created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                         updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE reviews
    ADD CONSTRAINT pk_reviews PRIMARY KEY (id);

ALTER TABLE reviews
    ADD CONSTRAINT uq_reviews_user_content
        UNIQUE (user_id, content_id);

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE reviews
    ADD CONSTRAINT fk_reviews_content
        FOREIGN KEY (content_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

ALTER TABLE reviews
    ADD CONSTRAINT chk_reviews_rating
        CHECK (
            rating BETWEEN 0.0 AND 5.0
                AND MOD(rating * 10, 5) = 0
            );

CREATE INDEX idx_reviews_content_created
    ON reviews (content_id, created_at DESC, id DESC);

CREATE INDEX idx_reviews_user_created
    ON reviews (user_id, created_at DESC, id DESC);


-- =================================================================
-- 플레이리스트

CREATE TABLE playlists (
                           id                          BINARY(16) NOT NULL,
                           owner_id                    BINARY(16) NOT NULL,
                           title                       VARCHAR(100) NOT NULL,
                           description                 TEXT NULL,
                           weekly_popularity_score     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
                           created_at                  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                           updated_at                  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE playlists
    ADD CONSTRAINT pk_playlists PRIMARY KEY (id);

ALTER TABLE playlists
    ADD CONSTRAINT fk_playlists_owner
        FOREIGN KEY (owner_id)
            REFERENCES users (id)
            ON DELETE RESTRICT;

ALTER TABLE playlists
    ADD CONSTRAINT chk_playlists_weekly_score
        CHECK (weekly_popularity_score >= 0);

CREATE INDEX idx_playlists_owner_created
    ON playlists (owner_id, created_at DESC);

CREATE INDEX idx_playlists_weekly_score
    ON playlists (weekly_popularity_score DESC, id DESC);


-- =================================================================
-- 플레이리스트 콘텐츠

CREATE TABLE playlist_contents (
                                   id                  BINARY(16) NOT NULL,
                                   playlist_id         BINARY(16) NOT NULL,
                                   content_id          BINARY(16) NOT NULL,
                                   created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE playlist_contents
    ADD CONSTRAINT pk_playlist_contents PRIMARY KEY (id);

ALTER TABLE playlist_contents
    ADD CONSTRAINT uq_playlist_contents
        UNIQUE (playlist_id, content_id);

ALTER TABLE playlist_contents
    ADD CONSTRAINT fk_playlist_contents_playlist
        FOREIGN KEY (playlist_id)
            REFERENCES playlists (id)
            ON DELETE CASCADE;

ALTER TABLE playlist_contents
    ADD CONSTRAINT fk_playlist_contents_content
        FOREIGN KEY (content_id)
            REFERENCES contents (id)
            ON DELETE CASCADE;

CREATE INDEX idx_playlist_contents_content
    ON playlist_contents (content_id);


-- =================================================================
-- 플레이리스트 태그

CREATE TABLE playlist_tags (
                               id                  BINARY(16) NOT NULL,
                               playlist_id         BINARY(16) NOT NULL,
                               tag_id              BINARY(16) NOT NULL
);

ALTER TABLE playlist_tags
    ADD CONSTRAINT pk_playlist_tags PRIMARY KEY (id);

ALTER TABLE playlist_tags
    ADD CONSTRAINT uq_playlist_tags
        UNIQUE (playlist_id, tag_id);

ALTER TABLE playlist_tags
    ADD CONSTRAINT fk_playlist_tags_playlist
        FOREIGN KEY (playlist_id)
            REFERENCES playlists (id)
            ON DELETE CASCADE;

ALTER TABLE playlist_tags
    ADD CONSTRAINT fk_playlist_tags_tag
        FOREIGN KEY (tag_id)
            REFERENCES tags (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_playlist_tags_tag
    ON playlist_tags (tag_id);

-- =================================================================
-- 사용자 플레이리스트 태그 선호도

CREATE TABLE user_playlist_tag_preferences (
                                               id                  BINARY(16) NOT NULL,
                                               user_id             BINARY(16) NOT NULL,
                                               tag_id              BINARY(16) NOT NULL,
                                               score               DOUBLE NOT NULL DEFAULT 0,
                                               updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE user_playlist_tag_preferences
    ADD CONSTRAINT pk_user_playlist_tag_preferences
        PRIMARY KEY (id);

ALTER TABLE user_playlist_tag_preferences
    ADD CONSTRAINT uq_user_playlist_tag_preferences
        UNIQUE (user_id, tag_id);

ALTER TABLE user_playlist_tag_preferences
    ADD CONSTRAINT fk_user_playlist_tag_preferences_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE user_playlist_tag_preferences
    ADD CONSTRAINT fk_user_playlist_tag_preferences_tag
        FOREIGN KEY (tag_id)
            REFERENCES tags (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_user_playlist_tag_preferences_tag
    ON user_playlist_tag_preferences (tag_id);


-- =================================================================
-- 플레이리스트 구독

CREATE TABLE playlist_subscriptions (
                                        id                  BINARY(16) NOT NULL,
                                        user_id             BINARY(16) NOT NULL,
                                        playlist_id         BINARY(16) NOT NULL,
                                        created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE playlist_subscriptions
    ADD CONSTRAINT pk_playlist_subscriptions PRIMARY KEY (id);

ALTER TABLE playlist_subscriptions
    ADD CONSTRAINT uq_playlist_subscriptions
        UNIQUE (user_id, playlist_id);

ALTER TABLE playlist_subscriptions
    ADD CONSTRAINT fk_playlist_subscriptions_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE playlist_subscriptions
    ADD CONSTRAINT fk_playlist_subscriptions_playlist
        FOREIGN KEY (playlist_id)
            REFERENCES playlists (id)
            ON DELETE CASCADE;

CREATE INDEX idx_playlist_subscriptions_playlist
    ON playlist_subscriptions (playlist_id, created_at);


-- =================================================================
-- Watch Party

CREATE TABLE watch_parties (
                               id                  BINARY(16) NOT NULL,
                               host_id             BINARY(16) NOT NULL,
                               content_id          BINARY(16) NOT NULL,
                               title               VARCHAR(100) NOT NULL,
                               description         TEXT NULL,
                               scheduled_at        DATETIME(6) NOT NULL,
                               status              ENUM('SCHEDULED', 'LIVE', 'ENDED') NOT NULL
                        DEFAULT 'SCHEDULED',
                               max_participants    INT UNSIGNED NOT NULL,

                               start_episode       INT NULL,
                               end_episode         INT NULL,
                               created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               ended_at            DATETIME(6) NULL
);

ALTER TABLE watch_parties
    ADD CONSTRAINT pk_watch_parties PRIMARY KEY (id);

ALTER TABLE watch_parties
    ADD CONSTRAINT fk_watch_parties_host
        FOREIGN KEY (host_id)
            REFERENCES users (id)
            ON DELETE RESTRICT;

ALTER TABLE watch_parties
    ADD CONSTRAINT fk_watch_parties_content
        FOREIGN KEY (content_id)
            REFERENCES contents (id)
            ON DELETE RESTRICT;

ALTER TABLE watch_parties
    ADD CONSTRAINT chk_watch_parties_max_participants
        CHECK (max_participants > 0);



ALTER TABLE watch_parties
    ADD CONSTRAINT chk_watch_parties_episode_range
        CHECK (
            (start_episode IS NULL AND end_episode IS NULL)
                OR
            (
                start_episode IS NOT NULL
                    AND end_episode IS NOT NULL
                    AND start_episode >= 0
                    AND end_episode >= start_episode
                )
            );

CREATE INDEX idx_watch_parties_content_schedule
    ON watch_parties (content_id, scheduled_at, id);

CREATE INDEX idx_watch_parties_status_schedule
    ON watch_parties (status, scheduled_at, id);


-- =================================================================
-- Watch Party 참가자

CREATE TABLE watch_party_participants (
                                          id                  BINARY(16) NOT NULL,
                                          user_id             BINARY(16) NOT NULL,
                                          watch_party_id      BINARY(16) NOT NULL,
                                          status              ENUM('JOINED', 'LEFT', 'KICKED') NOT NULL
                        DEFAULT 'JOINED',
                                          joined_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                          left_at             DATETIME(6) NULL
);

ALTER TABLE watch_party_participants
    ADD CONSTRAINT pk_watch_party_participants PRIMARY KEY (id);

ALTER TABLE watch_party_participants
    ADD CONSTRAINT uq_watch_party_participants
        UNIQUE (user_id, watch_party_id);

ALTER TABLE watch_party_participants
    ADD CONSTRAINT fk_watch_party_participants_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE watch_party_participants
    ADD CONSTRAINT fk_watch_party_participants_party
        FOREIGN KEY (watch_party_id)
            REFERENCES watch_parties (id)
            ON DELETE CASCADE;

CREATE INDEX idx_watch_party_participants_party
    ON watch_party_participants (watch_party_id, status);


-- =================================================================
-- Watch Party 시작 알림

CREATE TABLE watch_party_reminders (
                                       id                  BINARY(16) NOT NULL,
                                       watch_party_id      BINARY(16) NOT NULL,
                                       user_id             BINARY(16) NOT NULL,
                                       created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE watch_party_reminders
    ADD CONSTRAINT pk_watch_party_reminders PRIMARY KEY (id);

ALTER TABLE watch_party_reminders
    ADD CONSTRAINT uq_watch_party_reminders
        UNIQUE (watch_party_id, user_id);

ALTER TABLE watch_party_reminders
    ADD CONSTRAINT fk_watch_party_reminders_party
        FOREIGN KEY (watch_party_id)
            REFERENCES watch_parties (id)
            ON DELETE CASCADE;

ALTER TABLE watch_party_reminders
    ADD CONSTRAINT fk_watch_party_reminders_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

CREATE INDEX idx_watch_party_reminders_user
    ON watch_party_reminders (user_id);


-- =================================================================
-- DM 대화방

CREATE TABLE conversations (
                               id                  BINARY(16) NOT NULL,
                               created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE conversations
    ADD CONSTRAINT pk_conversations PRIMARY KEY (id);


-- =================================================================
-- DM 대화방 참여자

CREATE TABLE conversation_participants (
                                           id                  BINARY(16) NOT NULL,
                                           conversation_id     BINARY(16) NOT NULL,
                                           user_id             BINARY(16) NOT NULL,
                                           joined_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE conversation_participants
    ADD CONSTRAINT pk_conversation_participants PRIMARY KEY (id);

ALTER TABLE conversation_participants
    ADD CONSTRAINT uq_conversation_participants
        UNIQUE (conversation_id, user_id);

ALTER TABLE conversation_participants
    ADD CONSTRAINT fk_conversation_participants_conversation
        FOREIGN KEY (conversation_id)
            REFERENCES conversations (id)
            ON DELETE CASCADE;

ALTER TABLE conversation_participants
    ADD CONSTRAINT fk_conversation_participants_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

CREATE INDEX idx_conversation_participants_user
    ON conversation_participants (user_id);


-- =================================================================
-- DM 메시지

CREATE TABLE direct_messages (
                                 id                  BINARY(16) NOT NULL,
                                 conversation_id     BINARY(16) NOT NULL,
                                 sender_id           BINARY(16) NOT NULL,
                                 content             VARCHAR(255) NOT NULL,
                                 created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                                 read_at             DATETIME(6) NULL
);

ALTER TABLE direct_messages
    ADD CONSTRAINT pk_direct_messages PRIMARY KEY (id);

ALTER TABLE direct_messages
    ADD CONSTRAINT fk_direct_messages_conversation
        FOREIGN KEY (conversation_id)
            REFERENCES conversations (id)
            ON DELETE CASCADE;

ALTER TABLE direct_messages
    ADD CONSTRAINT fk_direct_messages_sender
        FOREIGN KEY (sender_id)
            REFERENCES users (id)
            ON DELETE RESTRICT;

CREATE INDEX idx_direct_messages_conversation_created
    ON direct_messages (conversation_id, created_at, id);

CREATE INDEX idx_direct_messages_sender
    ON direct_messages (sender_id);


-- =================================================================
-- 팔로우

CREATE TABLE follows (
                         id                  BINARY(16) NOT NULL,
                         follower_id         BINARY(16) NOT NULL,
                         followee_id         BINARY(16) NOT NULL
);

ALTER TABLE follows
    ADD CONSTRAINT pk_follows PRIMARY KEY (id);

ALTER TABLE follows
    ADD CONSTRAINT uq_follows
        UNIQUE (follower_id, followee_id);

ALTER TABLE follows
    ADD CONSTRAINT fk_follows_follower
        FOREIGN KEY (follower_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE follows
    ADD CONSTRAINT fk_follows_followee
        FOREIGN KEY (followee_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

ALTER TABLE follows
    ADD CONSTRAINT chk_follows_not_self
        CHECK (follower_id <> followee_id);

CREATE INDEX idx_follows_followee
    ON follows (followee_id);


-- =================================================================
-- 알림

CREATE TABLE notifications (
                               id                  BINARY(16) NOT NULL,
                               receiver_id         BINARY(16) NOT NULL,
                               title               VARCHAR(100) NOT NULL,
                               content             VARCHAR(500) NOT NULL,
                               level               ENUM('INFO', 'WARNING', 'ERROR') NOT NULL
                        DEFAULT 'INFO',
                               created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

ALTER TABLE notifications
    ADD CONSTRAINT pk_notifications PRIMARY KEY (id);

ALTER TABLE notifications
    ADD CONSTRAINT fk_notifications_receiver
        FOREIGN KEY (receiver_id)
            REFERENCES users (id)
            ON DELETE CASCADE;

CREATE INDEX idx_notifications_receiver_created
    ON notifications (receiver_id, created_at DESC, id DESC);