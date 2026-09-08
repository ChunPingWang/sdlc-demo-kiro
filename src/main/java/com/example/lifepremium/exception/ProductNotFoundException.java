package com.example.lifepremium.exception;

public class ProductNotFoundException extends BusinessException {
    public ProductNotFoundException(String productCode) {
        super(ErrorCode.PRODUCT_NOT_FOUND,
              String.format("查無商品：productCode=%s", productCode));
    }
}
