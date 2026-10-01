package com.moduplaylist.batch.job.contenttagging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moduplaylist.core.content.ai.ContentEvidenceResearcher;
import com.moduplaylist.core.content.ai.ContentExternalEvidence;
import com.moduplaylist.core.content.ai.ContentResearchInput;
import com.moduplaylist.core.content.ai.ContentTaggingException;
import com.moduplaylist.core.content.entity.Content;
import com.moduplaylist.core.content.entity.ContentType;
import com.moduplaylist.core.content.repository.ContentRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Research is performed between two short transactions, never while a content row is locked. */
public final class ContentExternalEvidenceService {
    private final ContentRepository contents;
    private final ContentEvidenceResearcher researcher;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final ContentTaggingProperties.Research settings;
    private final TransactionTemplate readTransaction;
    private final TransactionTemplate writeTransaction;

    public ContentExternalEvidenceService(ContentRepository contents, ContentEvidenceResearcher researcher,
                                          ObjectMapper mapper, PlatformTransactionManager transactionManager,
                                          ContentTaggingProperties properties) {
        this(contents, researcher, mapper, transactionManager, Clock.systemUTC(), properties);
    }

    ContentExternalEvidenceService(ContentRepository contents, ContentEvidenceResearcher researcher,
                                   ObjectMapper mapper, PlatformTransactionManager transactionManager, Clock clock) {
        this(contents, researcher, mapper, transactionManager, clock, new ContentTaggingProperties());
    }

    ContentExternalEvidenceService(ContentRepository contents, ContentEvidenceResearcher researcher,
                                   ObjectMapper mapper, PlatformTransactionManager transactionManager, Clock clock,
                                   ContentTaggingProperties properties) {
        this.contents = contents;
        this.researcher = researcher;
        this.mapper = mapper;
        this.clock = clock;
        this.settings = properties.getResearch();
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readTransaction.setReadOnly(true);
        this.writeTransaction = new TransactionTemplate(transactionManager);
        this.writeTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** False means this content became ineligible or changed during research; failures leave it PENDING. */
    public boolean ensureEvidence(UUID contentId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Content research must start outside a transaction");
        }
        Snapshot before = readTransaction.execute(status -> snapshot(contents.findById(contentId).orElse(null)));
        if (before == null) return false;
        if (!before.needsWebSearch()) return true;
        if (before.cached()) return true;

        List<ContentExternalEvidence> facts = List.copyOf(researcher.research(before.input()));
        validateFacts(facts, before.input());

        return Boolean.TRUE.equals(writeTransaction.execute(status -> {
            Content current = lockTarget(contentId);
            Snapshot after = snapshot(current);
            if (after == null || !after.needsWebSearch()
                || !before.identityHash().equals(after.identityHash())) return false;
            if (after.cached()) return true; // A concurrent worker already stored the same identity.
            current.mergeMetadata(Map.of(ContentResearchCache.METADATA_KEY,
                ContentResearchCache.value(before.identityHash(), facts, clock.instant())));
            return true;
        }));
    }

    private Content lockTarget(UUID contentId) {
        UUID parentId = contents.findParentId(contentId).orElse(null);
        Content lockedParent = parentId == null ? null : contents.findByIdForUpdate(parentId).orElse(null);
        Content content = contents.findByIdForUpdate(contentId).orElse(null);
        if (content == null) return null;
        if (content.getType() == ContentType.TV_SEASON && (lockedParent == null
            || content.getParentContent() == null
            || !parentId.equals(content.getParentContent().getId()))) return null;
        return content;
    }

    private Snapshot snapshot(Content content) {
        if (content == null || content.isHidden() || !"TMDB".equals(content.getExternalSource())
            || content.getExternalId() == null || content.getAiTaggingStatus() != Content.AiTaggingStatus.PENDING
            || (content.getType() != ContentType.MOVIE && content.getType() != ContentType.TV_SEASON)) return null;

        Content parent = content.getType() == ContentType.TV_SEASON ? content.getParentContent() : null;
        if (content.getType() == ContentType.TV_SEASON && (parent == null || parent.isHidden()
            || parent.getType() != ContentType.TV_SERIES || !"TMDB".equals(parent.getExternalSource())
            || parent.getExternalId() == null)) return null;

        if (!ContentResearchPolicy.needsWebSearch(content.getDescription())) {
            return new Snapshot(null, null, false, false);
        }
        ContentResearchInput input = ContentResearchCache.input(content);
        String identityHash = ContentResearchCache.identityHash(content, input, mapper);
        return new Snapshot(input, identityHash, true,
            ContentResearchCache.read(content, identityHash, clock.instant(), settings.getCacheDays(),
                settings.getMaxFacts(), settings.getMaxSources()) != null);
    }

    private void validateFacts(List<ContentExternalEvidence> facts, ContentResearchInput input) {
        if (facts.size() > settings.getMaxFacts()
            || facts.stream().map(ContentExternalEvidence::sourceUrl).distinct().count() > settings.getMaxSources()) {
            throw new ContentTaggingException("RESEARCH_INVALID_EVIDENCE", false, false);
        }
        for (ContentExternalEvidence fact : facts) {
            if (!ContentTagGuard.usableExternalEvidence(fact, input.type())) {
                throw new ContentTaggingException("RESEARCH_INVALID_EVIDENCE", false, false);
            }
        }
    }

    private record Snapshot(ContentResearchInput input, String identityHash,
                            boolean needsWebSearch, boolean cached) { }
}
