package com.moduplaylist.infrastructure.storage;

import java.util.UUID;

public interface UserProfileImageStorage {

	String upload(UUID userId, byte[] data, String contentType);

	void delete(UUID userId, String url);
}
