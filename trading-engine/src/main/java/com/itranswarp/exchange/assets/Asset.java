package com.itranswarp.exchange.assets;

import java.math.BigDecimal;

public class Asset {

    public BigDecimal available;

    public BigDecimal frozen;

    public Asset(BigDecimal available, BigDecimal frozen) {
        this.available = available;
        this.frozen = frozen;
    }

    public Asset() {
        this(BigDecimal.ZERO , BigDecimal.ZERO);
    }
}
