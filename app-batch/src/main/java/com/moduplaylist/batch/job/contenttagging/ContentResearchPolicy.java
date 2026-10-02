package com.moduplaylist.batch.job.contenttagging;

/** Web research is reserved for content whose usable description is at most 90 characters. */
final class ContentResearchPolicy {
    static final int MAX_DESCRIPTION_CHARACTERS = 90;

    private ContentResearchPolicy() { }

    static boolean needsWebSearch(String description) {
        String usableDescription = ContentTagGuard.inputText(description, 4000);
        return usableDescription.codePointCount(0, usableDescription.length())
            <= MAX_DESCRIPTION_CHARACTERS;
    }
}
