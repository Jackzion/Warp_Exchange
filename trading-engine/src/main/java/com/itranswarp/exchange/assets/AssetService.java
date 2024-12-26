package com.itranswarp.exchange.assets;

import com.itranswarp.exchange.LoggerSupport;
import com.itranswarp.exchange.enums.AssetEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AssetService extends LoggerSupport {

    // userID -> map(assetEnum -> asset[available/frozen])
    public final ConcurrentMap<Long, ConcurrentMap<AssetEnum,Asset>> userAssets = new ConcurrentHashMap<>();

    public Asset getAsset(Long userID , AssetEnum assetEnum){
        ConcurrentMap<AssetEnum, Asset> assets = userAssets.get(userID);
        if(assets == null) return null;
        return assets.get(assetEnum);
    }

    public ConcurrentMap<Long, ConcurrentMap<AssetEnum, Asset>> getUserAssets() {
        return this.userAssets;
    }

    public boolean tryTransfer(Transfer type , Long fromUser , Long toUser , AssetEnum assetEnum , BigDecimal amount , boolean checkBalance){
        if(amount.signum()<0) throw new IllegalArgumentException("Negative amount");
        // 获取源用户资产：
        Asset fromAsset = getAsset(fromUser, assetEnum);
        if(fromAsset == null) fromAsset = initAssets(fromUser,assetEnum);
        // 获取目标用户资产：
        Asset toAsset = getAsset(toUser, assetEnum);
        if(toAsset == null) toAsset = initAssets(toUser,assetEnum);
        return switch(type){
            case AVAILABLE_TO_AVAILABLE->{
                // 需要检查余额
                if(checkBalance && fromAsset.available.compareTo(amount) < 0){
                    yield  false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            case FROZEN_TO_AVAILABLE -> {
                if (checkBalance && fromAsset.frozen.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.frozen = fromAsset.frozen.subtract(amount);
                toAsset.available = toAsset.available.add(amount);
                yield true;
            }
            // 从可用转至冻结:
            case AVAILABLE_TO_FROZEN -> {
                if (checkBalance && fromAsset.available.compareTo(amount) < 0) {
                    yield false;
                }
                fromAsset.available = fromAsset.available.subtract(amount);
                toAsset.frozen = toAsset.frozen.add(amount);
                yield true;
            }
            default -> {
                throw new IllegalArgumentException("invalid type: " + type);
            }
        };
    }

    // 冻结
    public boolean tryFreeze(Long userID , AssetEnum assetEnum , BigDecimal amount){
        boolean ok = tryTransfer(Transfer.AVAILABLE_TO_FROZEN , userID,userID,assetEnum,amount,true);
        if (ok && logger.isDebugEnabled()) {
            logger.debug("freezed user {}, asset {}, amount {}", userID, assetEnum, amount);
        }
        return ok;
    }

    // 取消冻结
    public boolean unFreeze(Long userID , AssetEnum assetEnum , BigDecimal amount){
        boolean ok = tryTransfer(Transfer.FROZEN_TO_AVAILABLE , userID,userID,assetEnum,amount,true);
        if (ok && logger.isDebugEnabled()) {
            logger.debug("freezed user {}, asset {}, amount {}", userID, assetEnum, amount);
        }
        return ok;
    }

    public void transfer(Transfer type, Long fromUser, Long toUser, AssetEnum assetEnum, BigDecimal amount) {
        if (!tryTransfer(type, fromUser, toUser, assetEnum, amount, true)) {
            throw new RuntimeException("Transfer failed for " + type + ", from user " + fromUser + " to user " + toUser
                    + ", asset = " + assetEnum + ", amount = " + amount);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("transfer asset {}, from {} => {}, amount {}", assetEnum, fromUser, toUser, amount);
        }
    }

    private Asset initAssets(Long userID , AssetEnum assetEnum){
        ConcurrentMap<AssetEnum , Asset> map = userAssets.get(userID);
        if(map == null){
            map = new ConcurrentHashMap<AssetEnum , Asset>();
            userAssets.put(userID,map);
        }
        Asset zeroAsset = new Asset();
        map.put(assetEnum,zeroAsset);
        return zeroAsset;
    }

    public void debug() {
        System.out.println("---------- assets ----------");
        List<Long> userIds = new ArrayList<>(userAssets.keySet());
        Collections.sort(userIds);
        for (Long userId : userIds) {
            System.out.println("  user " + userId + " ----------");
            Map<AssetEnum, Asset> assets = userAssets.get(userId);
            List<AssetEnum> assetIds = new ArrayList<>(assets.keySet());
            Collections.sort(assetIds);
            for (AssetEnum assetId : assetIds) {
                System.out.println("    " + assetId + ": " + assets.get(assetId));
            }
        }
        System.out.println("---------- // assets ----------");
    }
}
