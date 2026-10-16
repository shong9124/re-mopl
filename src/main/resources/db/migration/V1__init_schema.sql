-- ---------- 1. 사용자 / 인증 ----------

CREATE TABLE users
(
    id               UUID PRIMARY KEY,
    email            VARCHAR(255) NOT NULL,
    password         VARCHAR(255),
    name             VARCHAR(100) COLLATE "ko-KR-x-icu" NOT NULL,
    profile_image_id UUID,
    role             VARCHAR(20)  NOT NULL DEFAULT 'USER',
    locked           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('USER', 'ADMIN'))
);

CREATE TABLE user_stats
(
    user_id         UUID PRIMARY KEY,
    follower_count  INTEGER     NOT NULL DEFAULT 0,
    following_count INTEGER     NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_user_stats_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE social_accounts
(
    id               UUID PRIMARY KEY,
    user_id          UUID         NOT NULL,
    provider         VARCHAR(20)  NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_social_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_social_provider CHECK (provider IN ('GOOGLE', 'KAKAO')),
    CONSTRAINT uk_social_accounts_provider_provider_user_id UNIQUE (provider, provider_user_id)
);
CREATE INDEX idx_social_user ON social_accounts (user_id);

CREATE TABLE temporary_passwords
(
    id            UUID PRIMARY KEY,
    user_id       UUID         NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    expires_at    TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_temppw_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_temporary_passwords_user_id UNIQUE (user_id)
);
CREATE INDEX idx_temppw_expires ON temporary_passwords (expires_at);

-- ---------- 2. 콘텐츠 / 태그 / 평가 ----------

CREATE TABLE contents
(
    id           UUID PRIMARY KEY,
    type         VARCHAR(20)  NOT NULL,
    title        VARCHAR(500) NOT NULL,
    description  TEXT,
    thumbnail_id UUID,
    released_at  TIMESTAMPTZ,
    metadata     JSONB,
    source       VARCHAR(20)  NOT NULL,
    external_id  VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_contents_type CHECK (type IN ('MOVIE', 'TV_SERIES', 'SPORT')),
    CONSTRAINT ck_contents_source CHECK (source IN ('TMDB', 'SPORTS_DB', 'ADMIN')),
    CONSTRAINT uk_contents_source_external_id UNIQUE (source, external_id)
);
CREATE INDEX idx_contents_type ON contents (type);
CREATE INDEX idx_contents_title ON contents (title);
CREATE INDEX idx_contents_metadata ON contents USING GIN (metadata);

CREATE TABLE content_stats
(
    id            UUID             PRIMARY KEY REFERENCES contents (id) ON DELETE CASCADE,
    review_count  INTEGER          NOT NULL DEFAULT 0,
    rating_sum    DOUBLE PRECISION NOT NULL DEFAULT 0,
    watcher_count BIGINT           NOT NULL DEFAULT 0
);

CREATE TABLE tags
(
    id   UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    CONSTRAINT uk_tags_name UNIQUE (name)
);

CREATE TABLE content_tags
(
    content_id UUID NOT NULL,
    tag_id     UUID NOT NULL,
    CONSTRAINT pk_content_tags PRIMARY KEY (content_id, tag_id),
    CONSTRAINT fk_ct_content FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE,
    CONSTRAINT fk_ct_tag FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE
);
CREATE INDEX idx_ct_tag ON content_tags (tag_id);

CREATE TABLE reviews
(
    id         UUID PRIMARY KEY,
    content_id UUID             NOT NULL,
    user_id    UUID             NOT NULL,
    text       TEXT,
    rating     DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ      NOT NULL,
    updated_at TIMESTAMPTZ      NOT NULL,
    CONSTRAINT fk_review_content FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_review_rating CHECK (rating >= 0 AND rating <= 5),
    CONSTRAINT uk_reviews_content_id_user_id UNIQUE (content_id, user_id)
);
CREATE INDEX idx_review_content ON reviews (content_id);
CREATE INDEX idx_review_user ON reviews (user_id);

-- ---------- 3. 플레이리스트 / 구독 ----------

CREATE TABLE playlists
(
    id               UUID PRIMARY KEY,
    owner_id         UUID         NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT         NOT NULL,
    subscriber_count INTEGER      NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_playlist_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX idx_playlist_owner ON playlists (owner_id);

CREATE TABLE playlist_items
(
    id          UUID PRIMARY KEY,
    playlist_id UUID        NOT NULL,
    content_id  UUID        NOT NULL,
    added_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_item_playlist FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_content FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE,
    CONSTRAINT uk_playlist_items_playlist_id_content_id UNIQUE (playlist_id, content_id)
);

CREATE TABLE playlist_subscriptions
(
    id            UUID PRIMARY KEY,
    playlist_id   UUID        NOT NULL,
    subscriber_id UUID        NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_sub_playlist FOREIGN KEY (playlist_id) REFERENCES playlists (id) ON DELETE CASCADE,
    CONSTRAINT fk_playlist_subscriptions_user_id FOREIGN KEY (subscriber_id) REFERENCES users (id),
    CONSTRAINT uk_playlist_subscriptions_playlist_id_subscriber_id UNIQUE (playlist_id, subscriber_id)
);
CREATE INDEX idx_sub_subscriber ON playlist_subscriptions (subscriber_id);

-- ---------- 4. 팔로우 / 실시간 시청 ----------

CREATE TABLE follows
(
    id          UUID PRIMARY KEY,
    follower_id UUID        NOT NULL,
    followee_id UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_follows_follower_id_followee_id UNIQUE (follower_id, followee_id),
    CONSTRAINT ck_follow_self CHECK (follower_id <> followee_id)
);
CREATE INDEX idx_follow_followee ON follows (followee_id);

CREATE TABLE watching_sessions
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL,
    content_id UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_watch_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_watch_content FOREIGN KEY (content_id) REFERENCES contents (id) ON DELETE CASCADE
);
CREATE UNIQUE INDEX uk_watching_sessions_user_id ON watching_sessions (user_id);
CREATE INDEX uk_watching_sessions_content_id ON watching_sessions (content_id);

-- ---------- 5. DM (쪽지) ----------

CREATE TABLE conversations
(
    id           UUID PRIMARY KEY,
    participant1 UUID        NOT NULL,
    participant2 UUID        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_conv_p1 FOREIGN KEY (participant1) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_conv_p2 FOREIGN KEY (participant2) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_conv_order CHECK (participant1 < participant2),
    CONSTRAINT uk_conversations_participant1_participant2 UNIQUE (participant1, participant2)
);
CREATE INDEX idx_conv_p1 ON conversations (participant1);
CREATE INDEX idx_conv_p2 ON conversations (participant2);

CREATE TABLE direct_messages
(
    id              UUID PRIMARY KEY,
    conversation_id UUID        NOT NULL,
    sender_id       UUID        NOT NULL,
    receiver_id     UUID        NOT NULL,
    content         VARCHAR(1000) NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_dm_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_dm_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_dm_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_dm_read_state CHECK (
        (is_read = FALSE AND read_at IS NULL) OR
        (is_read = TRUE AND read_at IS NOT NULL)
        )
);
CREATE INDEX idx_dm_conversation ON direct_messages (conversation_id, created_at);
CREATE INDEX idx_dm_conversation_receiver_unread ON direct_messages (conversation_id, receiver_id, created_at) WHERE is_read = false;

-- ---------- 6. 알림 (SSE) ----------

CREATE TABLE notifications
(
    id          UUID PRIMARY KEY,
    receiver_id UUID         NOT NULL,
    type        VARCHAR(30)  NOT NULL,
    title       VARCHAR(255) NOT NULL,
    content     TEXT,
    level       VARCHAR(20)  NOT NULL DEFAULT 'INFO',
    is_read     BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_noti_receiver FOREIGN KEY (receiver_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_noti_level CHECK (level IN ('INFO', 'WARNING', 'ERROR')),
    CONSTRAINT ck_noti_type CHECK (type IN (
                                            'ROLE_CHANGED',
                                            'PLAYLIST_SUBSCRIBED',
                                            'PLAYLIST_UPDATED',
                                            'FOLLOWED',
                                            'DIRECT_MESSAGE',
                                            'WATCHING_ACTIVITY'
        )),
    CONSTRAINT ck_noti_read_state CHECK (
        (is_read = FALSE AND read_at IS NULL) OR
        (is_read = TRUE AND read_at IS NOT NULL)
        )
);
CREATE INDEX idx_noti_receiver_created_id ON notifications (receiver_id, created_at DESC, id DESC);
CREATE INDEX idx_noti_unread ON notifications (receiver_id) WHERE is_read = FALSE;
