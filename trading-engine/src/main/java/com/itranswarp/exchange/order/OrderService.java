package com.itranswarp.exchange.order;

import com.itranswarp.exchange.assets.AssetService;
import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.enums.Direction;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class OrderService {
    final AssetService assetService;

    // 跟踪所有活动订单 ， OrderID => OrderEntity
    final ConcurrentMap<Long,OrderEntity> activeOrders = new ConcurrentHashMap<>();
    // 跟踪所有用户订单 , UserID => MAP(OrderID => OrderEntity)
    final ConcurrentMap<Long , ConcurrentMap<Long,OrderEntity>> userOrders = new ConcurrentHashMap<>();

    public OrderEntity createOrder(long sequenceId, long ts, Long orderId, Long userId, Direction direction, BigDecimal price, BigDecimal quantity){
        switch (direction){
            case BUY -> {
                // 买入 ，冻结 USD
                if(!assetService.tryFreeze(userId, AssetEnum.USD,price.multiply(quantity))){
                    return null;
                }
            }
            case SELL -> {
                // 卖出 ，冻结 BTC
                if(!assetService.tryFreeze(userId, AssetEnum.BTC,quantity)){
                    return null;
                }
            }
            default -> throw new IllegalArgumentException("invalid direction");
        }
        // make order
        OrderEntity order = new OrderEntity();
        order.ID = orderId;
        order.sequenceID = sequenceId;
        order.userID = userId;
        order.direction = direction;
        order.price = price;
        order.quantity = quantity;
        order.unfilledQuantity = quantity;
        order.createAt = order.updateAt = ts;
        // 添加到 map
        this.activeOrders.put(orderId,order);
        ConcurrentMap<Long , OrderEntity> uOrders = this.userOrders.get(userId);
        if(uOrders==null){
            uOrders = new ConcurrentHashMap<>();
            this.userOrders.put(userId,uOrders);
        }
        uOrders.put(orderId,order);
        return order;
    }

    // 清算
    public void removeOrder(Long orderID){
        OrderEntity removed = this.activeOrders.remove(orderID);
        if(removed == null){
            throw new IllegalArgumentException("order not found in active orders");
        }
        ConcurrentMap<Long, OrderEntity> uorders = this.userOrders.get(removed.userID);
        if(uorders == null){
            throw new IllegalArgumentException("user not found");
        }
        if(uorders.remove(orderID) == null){
            throw new IllegalArgumentException("order not found in user's orders");
        }
    }

    // 根据订单ID查询Order，不存在返回null:
    public OrderEntity getOrder(Long orderId) {
        return this.activeOrders.get(orderId);
    }
    // 根据用户ID查询用户所有活动Order，不存在返回null:
    public ConcurrentMap<Long, OrderEntity> getUserOrders(Long userId) {
        return this.userOrders.get(userId);
    }

    public OrderService(AssetService assetService) {
        this.assetService = assetService;
    }
}
