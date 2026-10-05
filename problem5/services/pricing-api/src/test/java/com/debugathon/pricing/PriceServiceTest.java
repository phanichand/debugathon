package com.debugathon.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PriceServiceTest {
    final PriceRepository repository = mock(PriceRepository.class);
    final SharedPrices shared = mock(SharedPrices.class);
    final QuoteAssembler quotes = mock(QuoteAssembler.class);
    final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    final LocalPrices local = new LocalPrices(Duration.ofMinutes(1), meters);
    final PriceService service = new PriceService(repository, local, shared, quotes, meters);
    final Price initial = new Price("A", new BigDecimal("820.00"), "INR", 1, Instant.EPOCH);
    final Price next = new Price("A", new BigDecimal("950.00"), "INR", 2, Instant.EPOCH.plusSeconds(1));

    @BeforeEach void setup() {
        when(quotes.prepare(any())).thenAnswer(call -> {
            Price p = call.getArgument(0);
            return new Quote(p, p.amount(), BigDecimal.ZERO, "base");
        });
    }
    @Test void databaseResultIsReusedForSequentialReads() {
        when(repository.find("A")).thenReturn(initial);
        assertThat(service.get("A")).isEqualTo(initial);
        assertThat(service.get("A")).isEqualTo(initial);
        verify(repository, times(1)).find("A");
        verify(shared).offer(initial);
    }
    @Test void redisReadDoesNotReachDatabaseOrQuotePreparation() {
        when(shared.get("A")).thenReturn(initial);
        assertThat(service.get("A")).isEqualTo(initial);
        assertThat(service.get("A")).isEqualTo(initial);
        verifyNoInteractions(repository, quotes);
    }
    @Test void sequentialUpdatesRemainVisible() {
        local.put(initial);
        when(repository.update("A", next.amount(), 1)).thenReturn(next);
        assertThat(service.update("A", next.amount(), 1)).isEqualTo(next);
        assertThat(service.get("A")).isEqualTo(next);
        verify(shared).publish(next);
    }
    @Test void failedUpdateDoesNotTouchCaches() {
        local.put(initial);
        when(repository.update("A", next.amount(), 1)).thenThrow(new IllegalStateException("unavailable"));
        assertThatThrownBy(() -> service.update("A", next.amount(), 1)).isInstanceOf(IllegalStateException.class);
        assertThat(service.get("A")).isEqualTo(initial);
        verifyNoInteractions(shared);
    }
}
