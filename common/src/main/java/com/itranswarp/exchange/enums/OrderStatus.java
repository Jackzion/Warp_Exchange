package com.itranswarp.exchange.enums;

public enum OrderStatus {

    // 等待成交
    PENDING(false),
    // 完全成交
    FULLY_FILLED(true),
    // 部分成交
    PARTIAL_FILLED(false),
    // 部分成交后取消
    PARTIAL_CANCEL(true),
    // 完全取消
    FULLY_CANCELED(true);

    public final boolean isFinalStatus;

    OrderStatus(boolean isFinalStatus) {
        this.isFinalStatus = isFinalStatus;
    }
}
