package com.itranswarp.exchange.enums;

public enum Direction {
    // 购入
    BUY(1),
    // 卖出
    SELL(0);

    // get nagate direction
    public Direction negate(){
        return this == BUY ? SELL : BUY;
    }

    public final int value;

    Direction(int value) {
        this.value = value;
    }

    public static Direction of(int intValue){
        for(Direction direction : Direction.values()){
            if(direction.value == intValue) return direction;
        }
        throw new IllegalArgumentException("invalid Direction values");
    }
}
