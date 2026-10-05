package com.debugathon.pricing;

import java.math.BigDecimal;
import java.time.Instant;

public record Price(String productId, BigDecimal amount, String currency, long version, Instant updatedAt) {}
