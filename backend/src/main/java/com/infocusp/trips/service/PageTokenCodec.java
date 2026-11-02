package com.infocusp.trips.service;

import com.infocusp.trips.domain.InvalidRequestException;
import com.infocusp.trips.domain.PageCursor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/** Converts a cursor to and from an opaque URL-safe page token. */
public final class PageTokenCodec {
    private static final char SEPARATOR = '|';

    public String encode(PageCursor cursor) {
        String raw = cursor.createdAt() + String.valueOf(SEPARATOR) + cursor.tripId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public PageCursor decode(String token) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
            int split = raw.indexOf(SEPARATOR);
            return new PageCursor(Instant.parse(raw.substring(0, split)), raw.substring(split + 1));
        } catch (RuntimeException e) {
            throw new InvalidRequestException("invalid page_token");
        }
    }
}
