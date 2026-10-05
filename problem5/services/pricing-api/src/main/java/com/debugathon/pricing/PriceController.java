package com.debugathon.pricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/prices")
public class PriceController {
    private final PriceService prices;
    public PriceController(PriceService prices) { this.prices = prices; }
    @GetMapping("/{id}") public Price get(@PathVariable String id) { return prices.get(id); }
    @PutMapping("/{id}") public Price update(@PathVariable String id, @Valid @RequestBody Update update) {
        return prices.update(id, update.amount(), update.expectedVersion());
    }
    public record Update(@NotNull @DecimalMin("0.01") @DecimalMax("1000000.00")
                         @Digits(integer = 7, fraction = 2) BigDecimal amount,
                         @NotNull @Min(1) Long expectedVersion) {}
}
