package com.moduplaylist.api.content.service;

import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.infrastructure.embedding.EmbeddingGenerator;
import com.moduplaylist.infrastructure.opensearch.content.ContentSimilarityCandidate;
import com.moduplaylist.infrastructure.opensearch.content.ContentVectorSearchRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSemanticSearchService {

	private final ObjectProvider<EmbeddingGenerator> embeddingGeneratorProvider;
	private final ObjectProvider<ContentVectorSearchRepository> vectorSearchRepositoryProvider;
	private final ContentSemanticSearchProperties properties;

	public List<ContentSimilarityCandidate> search(String query, ContentType contentType) {
		if (!isEligible(query, contentType)) {
			return List.of();
		}

		EmbeddingGenerator embeddingGenerator = embeddingGeneratorProvider.getIfAvailable();
		ContentVectorSearchRepository vectorSearchRepository =
			vectorSearchRepositoryProvider.getIfAvailable();
		if (embeddingGenerator == null || vectorSearchRepository == null) {
			return List.of();
		}

		try {
			float[] queryVector = embeddingGenerator.embed(query.strip());
			return vectorSearchRepository.findNearest(
				queryVector,
				contentType,
				List.of(),
				properties.getCandidateLimit()
			);
		} catch (RuntimeException exception) {
			log.warn("의미 검색에 실패해 키워드 검색 결과만 사용합니다.", exception);
			return List.of();
		}
	}

	private boolean isEligible(String query, ContentType contentType) {
		if (!properties.isEnabled() || query == null || query.isBlank()) {
			return false;
		}
		if (contentType != null && !contentType.isPersonalizable()) {
			return false;
		}
		long queryLength = query.codePoints()
			.filter(codePoint -> !Character.isWhitespace(codePoint))
			.count();
		return queryLength >= properties.getMinimumQueryLength();
	}
}
