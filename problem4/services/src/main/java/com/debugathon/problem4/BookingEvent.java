package com.debugathon.problem4;
public record BookingEvent(String eventId, String bookingId, String merchantId,
                           long amountPaise, String currency, String runId) {
    public void validate() {
        if (eventId == null || eventId.isBlank() || bookingId == null || bookingId.isBlank()
            || merchantId == null || merchantId.isBlank() || runId == null || runId.isBlank()
            || amountPaise <= 0 || !"INR".equals(currency))
            throw new IllegalArgumentException("Valid event, booking, merchant, run, positive amount and INR required");
        if (eventId.length()>120 || bookingId.length()>120 || merchantId.length()>120 || runId.length()>120)
            throw new IllegalArgumentException("Identifiers must be <=120 characters");
    }
}
