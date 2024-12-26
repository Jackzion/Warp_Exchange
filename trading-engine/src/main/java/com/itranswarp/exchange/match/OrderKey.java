package com.itranswarp.exchange.match;

import java.math.BigDecimal;

public record OrderKey(long sequenceID , BigDecimal price) {

}
