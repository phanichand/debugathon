package com.debugathon.problem4;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BookingEventTest {
    @Test void acceptsBusinessEvent() { assertDoesNotThrow(()->new BookingEvent("e","b","m",82000,"INR","run").validate()); }
    @Test void rejectsMissingIdentityAndInvalidAmount() {
        assertThrows(IllegalArgumentException.class,()->new BookingEvent("","b","m",82000,"INR","run").validate());
        assertThrows(IllegalArgumentException.class,()->new BookingEvent("e","b","m",-1,"INR","run").validate());
    }
}
