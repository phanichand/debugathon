package com.debugathon.pricing;

import java.math.BigDecimal;

public record Quote(Price price, BigDecimal netAmount, BigDecimal taxAmount, String policyId) {}
