package com.infocusp.trips.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.infocusp.trips.domain.InvalidRequestException;
import com.infocusp.trips.domain.PageCursor;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class PageTokenCodecTest {
    private final PageTokenCodec codec = new PageTokenCodec();

    @Test
    void roundTripsCursor() {
        PageCursor cursor = new PageCursor(Instant.parse("2026-11-01T10:15:30.123456789Z"), "id|with|pipes");
        assertEquals(cursor, codec.decode(codec.encode(cursor)));
    }

    @Test
    void rejectsNonBase64() {
        assertThrows(InvalidRequestException.class, () -> codec.decode("!!!"));
    }

    @Test
    void rejectsMissingSeparator() {
        assertThrows(InvalidRequestException.class, () -> codec.decode(encodeRaw("no-separator")));
    }

    @Test
    void rejectsBadTimestamp() {
        assertThrows(InvalidRequestException.class, () -> codec.decode(encodeRaw("yesterday|id")));
    }

    private static String encodeRaw(String raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
