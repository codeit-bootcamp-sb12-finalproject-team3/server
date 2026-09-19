package com.moduplaylist.infrastructure.storage;

public interface ContentImageStorage {

	String upload(String objectKey, byte[] data, String contentType);

	void delete(String objectKey);
}
