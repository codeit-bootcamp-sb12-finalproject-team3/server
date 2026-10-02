package com.moduplaylist.core.content.ai;

import java.util.List;

public interface ContentEvidenceResearcher {
    /** An empty list means research succeeded but found no usable evidence; failures are exceptions. */
    List<ContentExternalEvidence> research(ContentResearchInput input);
}
