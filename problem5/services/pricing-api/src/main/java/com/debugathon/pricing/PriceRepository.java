package com.debugathon.pricing;

import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Repository
public class PriceRepository {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final RowMapper<Price> mapper = (rs, row) -> new Price(rs.getString("product_id"),
        rs.getBigDecimal("amount"), rs.getString("currency"), rs.getLong("version"),
        rs.getTimestamp("updated_at").toInstant());

    public PriceRepository(JdbcTemplate jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc; this.transactions = transactions;
    }

    public Price find(String id) {
        var rows = jdbc.query("SELECT * FROM product_prices WHERE product_id = ?", mapper, id);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unknown product");
        Price price = rows.getFirst();
        Events.emit("price.read", price, "source", "postgres");
        return price;
    }

    public Price update(String id, BigDecimal amount, long expectedVersion) {
        Price price = transactions.execute(status -> {
            var rows = jdbc.query("""
                UPDATE product_prices SET amount = ?, version = version + 1, updated_at = clock_timestamp()
                WHERE product_id = ? AND version = ? RETURNING *
                """, mapper, amount, id, expectedVersion);
            if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Version mismatch or unknown product");
            return rows.getFirst();
        });
        Events.emit("price.committed", price);
        return price;
    }
}
