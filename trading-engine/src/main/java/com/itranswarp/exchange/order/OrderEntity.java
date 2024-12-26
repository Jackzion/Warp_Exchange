package com.itranswarp.exchange.order;

import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;

import java.math.BigDecimal;

// todo : database Entity reference
public class OrderEntity {

    // 订单ID / 定序ID / 用户ID:
    public Long ID;
    public long sequenceID;
    public Long userID;

    // 价格/方向/状态
    public BigDecimal price;
    public Direction direction;
    public OrderStatus orderStatus;

    // 订单数量/未成交数量
    public BigDecimal quantity;
    public BigDecimal unfilledQuantity;

    // 创建时间 ， 更新时间
    public long createAt;
    public long updateAt;

    public void updateOrder(BigDecimal unfilledQuantity, OrderStatus status, long updatedAt) {
        this.unfilledQuantity = unfilledQuantity;
        this.orderStatus = status;
        this.updateAt = updatedAt;
    }

}
