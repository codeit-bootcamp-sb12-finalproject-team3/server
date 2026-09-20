package com.moduplaylist.api.content.service;

import com.moduplaylist.api.content.dto.ContentCreateRequest;
import com.moduplaylist.api.content.dto.ContentCreateResponse;
import com.moduplaylist.api.content.dto.ContentResponse;
import com.moduplaylist.api.content.dto.ContentUpdateRequest;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface ContentCommandService {

	ContentCreateResponse create(ContentCreateRequest request, MultipartFile thumbnail);

	ContentResponse update(UUID contentId, ContentUpdateRequest request, MultipartFile thumbnail);

	void delete(UUID contentId);
}
