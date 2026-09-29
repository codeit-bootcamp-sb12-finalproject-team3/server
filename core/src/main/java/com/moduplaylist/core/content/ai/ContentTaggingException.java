package com.moduplaylist.core.content.ai;

/** Safe reason only: provider bodies and prompts must not be logged. */
public class ContentTaggingException extends RuntimeException {
    private final boolean retryable;
    private final boolean abortJob;
    private final long retryAfterMillis;

    public ContentTaggingException(String reason, boolean retryable, boolean abortJob) {
        this(reason, retryable, abortJob, 0);
    }

    public ContentTaggingException(String reason, boolean retryable, boolean abortJob,
                                  long retryAfterMillis) {
        super(reason);
        this.retryable = retryable;
        this.abortJob = abortJob;
        this.retryAfterMillis = retryAfterMillis;
    }

    public boolean isRetryable() { return retryable; }
    public boolean isAbortJob() { return abortJob; }
    public long getRetryAfterMillis() { return retryAfterMillis; }
}
