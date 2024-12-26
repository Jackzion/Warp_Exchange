package com.itranswarp.exchange.match;

import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.enums.OrderStatus;
import com.itranswarp.exchange.order.OrderEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MatchEngine {

    // 买盘
    public final OrderBook buyBook = new OrderBook(Direction.BUY);

    // 卖盘
    public final OrderBook sellBook = new OrderBook(Direction.SELL);

    // 最新市场价
    public BigDecimal marketPrice = BigDecimal.ZERO;

    // 上次处理 sequenceID
    private long sequenceID;

    public MatchResult processOrder(long sequenceID , OrderEntity order){
        switch (order.direction){
            case BUY -> {
                // 买单与 sellBook 匹配 ，剩余放入 buyBook
                return processOrder( order.sequenceID, order , this.sellBook , this.buyBook);
            }
            case SELL -> {
                // 卖单与 buyBook 匹配 ，剩余放入 sellBook
                return processOrder( order.sequenceID, order , this.buyBook , this.sellBook);
            }
            default -> throw new IllegalArgumentException("invalid direction ");
        }
    }

    MatchResult processOrder(long sequenceId, OrderEntity takerOrder, OrderBook makerBook, OrderBook anotherBook) {
        this.sequenceID = sequenceId;
        long ts = takerOrder.createAt;
        MatchResult matchResult = new MatchResult(takerOrder);
        BigDecimal takerUnfilledQuantity = takerOrder.quantity;
        // 与 makerBook 匹配价格
        for(;;){
            OrderEntity makerOrder = makerBook.getFirst();
            if(makerOrder == null) break;
            // 买入 ，但卖盘第一档价格过高
            if(takerOrder.direction == Direction.BUY && takerOrder.price.compareTo(makerOrder.price)<0){
                break;
            }
            // 卖出 ，但买盘第一档价格过低
            if(takerOrder.direction == Direction.SELL && takerOrder.price.compareTo(makerOrder.price)>0){
                break;
            }
            // 以 maker 价格成交
            this.marketPrice = makerOrder.price;
            // 成交数量为两者最小值
            BigDecimal matchQuantity = takerUnfilledQuantity.min(makerOrder.quantity);
            // 添加成交记录
            matchResult.add(this.marketPrice,matchQuantity,makerOrder);
            // 更新订单数量
            takerUnfilledQuantity  = takerUnfilledQuantity.subtract(matchQuantity);
            BigDecimal makerUnfilledQuantity = makerOrder.quantity.subtract(matchQuantity);
            // 完全成交后 ，从订单本删除
            if(makerUnfilledQuantity.signum() == 0){
                makerOrder.updateOrder(makerUnfilledQuantity, OrderStatus.FULLY_FILLED,ts);
                makerBook.remove(makerOrder);
            }else{
                // 对手盘成交完
                makerOrder.updateOrder(makerUnfilledQuantity,OrderStatus.PARTIAL_FILLED,ts);
            }
            // Taker 成交完 ， break
            if(takerUnfilledQuantity.signum() == 0 ){
                takerOrder.updateOrder(takerUnfilledQuantity,OrderStatus.FULLY_FILLED,ts);
                break;
            }
        }
        // Taker 订单未完成成交 ， 放入 book
        if(takerUnfilledQuantity.signum() > 0) {
            takerOrder.updateOrder(takerUnfilledQuantity,
                    takerUnfilledQuantity.compareTo(takerOrder.quantity) == 0 ? OrderStatus.PENDING : OrderStatus.PARTIAL_FILLED,
                    ts);
            anotherBook.add(takerOrder);
        }
        return matchResult;
    }

}
