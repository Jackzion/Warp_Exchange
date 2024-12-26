package com.itranswarp.exchange.clearing;

import com.itranswarp.exchange.assets.AssetService;
import com.itranswarp.exchange.assets.Transfer;
import com.itranswarp.exchange.enums.AssetEnum;
import com.itranswarp.exchange.match.MatchDetailRecord;
import com.itranswarp.exchange.match.MatchResult;
import com.itranswarp.exchange.order.OrderEntity;
import com.itranswarp.exchange.order.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ClearingService {

//    todo: 手续费清算
//    @Value("${exchange.fee-rate:0.0005}")
//    BigDecimal feeRate;

    final AssetService assetService;

    final OrderService orderService;


    public ClearingService(@Autowired AssetService assetService, @Autowired OrderService orderService) {
        this.assetService = assetService;
        this.orderService = orderService;
    }

    public void clearMatchResult(MatchResult matchResult){
        OrderEntity taker = matchResult.takerOrder;
        switch (taker.direction){
            case BUY -> {
                // 买入 ， 按 maker 成交
                for(MatchDetailRecord detailRecord : matchResult.matchDetails){
                    OrderEntity maker = detailRecord.makerOrder();
                    BigDecimal matchedQuantity = detailRecord.quantity();
                    if(taker.price.compareTo(maker.price)>0){
                        // 买入价格低 ，返还部分 USD 给 taker
                        BigDecimal unfreezeQuote = taker.price.subtract(maker.price).multiply(matchedQuantity);
                        assetService.unFreeze(taker.userID, AssetEnum.USD,unfreezeQuote);
                    }
                    // 买入 USD 转给 卖方
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE , taker.userID, maker.userID , AssetEnum.USD , matchedQuantity.multiply(maker.price));
                    // 卖方 BTC 转给 买方
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, maker.userID,taker.userID , AssetEnum.BTC , matchedQuantity);
                    // 删除完全成交的 maker
                    if(maker.unfilledQuantity.signum() == 0){
                        orderService.removeOrder(maker.ID);
                    }
                }
                // 删除完全成交的 taker
                if(taker.unfilledQuantity.signum() == 0){
                    orderService.removeOrder(taker.ID);
                }
            }
            case SELL -> {
                // 卖方 ，按 maker 成交
                for(MatchDetailRecord detailRecord : matchResult.matchDetails){
                    OrderEntity maker = detailRecord.makerOrder();
                    BigDecimal matchedQuantity = detailRecord.quantity();
                    // BTC 出库
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE, taker.userID,maker.userID , AssetEnum.BTC , matchedQuantity);
                    // USD 入库
                    assetService.transfer(Transfer.FROZEN_TO_AVAILABLE , maker.userID, taker.userID , AssetEnum.USD , matchedQuantity.multiply(maker.price));
                    // 删除完全成交的Maker:
                    if (maker.unfilledQuantity.signum() == 0) {
                        orderService.removeOrder(maker.ID);
                    }
                }
                // 删除完全成交的 taker
                if(taker.unfilledQuantity.signum() == 0){
                    orderService.removeOrder(taker.ID);
                }
            }
            default -> throw new IllegalArgumentException("invalid Direction");
        }
    }

    // 取消订单
    public void clearCancelOrder(OrderEntity order){
        switch (order.direction){
            case BUY -> {
                // 解冻 USD  =  价格 x 未成交数量
                assetService.unFreeze(order.userID,AssetEnum.USD,order.price.multiply(order.unfilledQuantity));
            }
            case SELL -> {
                // 解冻 BTC
                assetService.unFreeze(order.userID,AssetEnum.USD,order.unfilledQuantity);
            }
            default -> throw new IllegalArgumentException("invalid Direction");
        }
        // 从OrderService中删除订单:
        orderService.removeOrder(order.ID);
    }

}
