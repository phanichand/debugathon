package com.debugathon.pricing;

import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class QuoteAssembler {
    private final JdbcTemplate jdbc;
    private final ThreadPoolExecutor executor;
    public QuoteAssembler(JdbcTemplate jdbc, @Value("${pricing.quote-workers}") int workers) {
        this.jdbc = jdbc;
        executor = new ThreadPoolExecutor(workers, workers, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(512), new ThreadPoolExecutor.CallerRunsPolicy());
    }
    public Quote prepare(Price price) {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> previous = MDC.getCopyOfContextMap();
            if (context != null) MDC.setContextMap(context);
            try { return calculate(price); }
            finally {
                if (previous == null) MDC.clear();
                else MDC.setContextMap(previous);
            }
        }, executor).join();
    }
    private Quote calculate(Price price) {
        var rules = jdbc.query("""
            SELECT rule_id, lower_amount, upper_amount, rate, priority
            FROM tax_rules WHERE market = 'IN' ORDER BY priority DESC
            """, (rs, row) -> new Rule(rs.getString(1), rs.getBigDecimal(2), rs.getBigDecimal(3),
                                      rs.getBigDecimal(4), rs.getInt(5)));
        Rule selected = rules.stream().filter(r -> price.amount().compareTo(r.lower()) >= 0
            && price.amount().compareTo(r.upper()) < 0).findFirst().orElseThrow();
        BigDecimal net = price.amount().divide(BigDecimal.ONE.add(selected.rate()), 2, RoundingMode.HALF_UP);
        Events.emit("quote.prepared", price, "policyId", selected.id(),
            "netAmount", net, "taxAmount", price.amount().subtract(net));
        return new Quote(price, net, price.amount().subtract(net), selected.id());
    }
    @PreDestroy public void close() { executor.shutdown(); }
    private record Rule(String id, BigDecimal lower, BigDecimal upper, BigDecimal rate, int priority) {}
}
