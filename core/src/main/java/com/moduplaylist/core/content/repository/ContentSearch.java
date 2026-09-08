package com.moduplaylist.core.content.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ContentSearch(String type, UUID genreId, String sportType, String keyword,
        UUID likedByUserId, boolean ratingSort, boolean ascending,
        Instant cursorTime, BigDecimal cursorRating, UUID idAfter, int limit) {}
