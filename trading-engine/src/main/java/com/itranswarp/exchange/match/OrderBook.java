package com.itranswarp.exchange.match;

import com.itranswarp.exchange.enums.Direction;
import com.itranswarp.exchange.order.OrderEntity;

import java.util.Comparator;
import java.util.TreeMap;

public class OrderBook {

    public final Direction direction;

    public final TreeMap<OrderKey , OrderEntity> book;

    public OrderBook(Direction direction) {
        this.direction = direction;
        this.book = new TreeMap<>(direction == Direction.BUY ? SORT_BUY : SORT_SELL);
    }

    public OrderEntity getFirst(){
        return this.book.isEmpty() ? null : this.book.firstEntry().getValue();
    }

    public boolean remove(OrderEntity order){
        return this.book.remove(new OrderKey(order.sequenceID , order.price)) != null;
    }

    public boolean add(OrderEntity order){
        return this.book.put(new OrderKey(order.sequenceID , order.price) , order) != null;
    }

    // 卖盘排序
    private static final Comparator<OrderKey> SORT_SELL = new Comparator<OrderKey>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            // 价格低 ， 时间早
            int cmp = o1.price().compareTo(o2.price());
            return cmp == 0 ? Long.compare(o1.sequenceID(),o2.sequenceID()) : cmp ;
        }
    };

    // 买盘排序
    private static final Comparator<OrderKey> SORT_BUY = new Comparator<OrderKey>() {
        @Override
        public int compare(OrderKey o1, OrderKey o2) {
            // 价格高 ， 时间早
            int cmp = o2.price().compareTo(o1.price());
            return cmp == 0 ? Long.compare(o1.sequenceID(),o2.sequenceID()) : cmp ;
        }
    };
}
