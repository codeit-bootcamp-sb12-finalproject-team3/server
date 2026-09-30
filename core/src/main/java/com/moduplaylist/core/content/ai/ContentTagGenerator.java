package com.moduplaylist.core.content.ai;

public interface ContentTagGenerator {
    /** Exactly one model request. Retry is owned by the batch. */
    String generate(ContentTagInput input, boolean repair);

    String modelName();
}
