package com.company.order.domain.valueobject;

import com.company.order.application.exception.BusinessException;
import com.company.order.application.exception.ErrorCode;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * Money Value Object。
 *
 * 使用 record 確保不可變、自動實作 equals / hashCode / toString。
 */
public record Money(BigDecimal amount, Currency currency) {

    public static final Money ZERO = new Money(BigDecimal.ZERO, Currency.getInstance("TWD"));

    public Money {
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("amount 與 currency 不可為 null");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("金額不可為負數");
        }
    }

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new BusinessException(ErrorCode.CURRENCY_MISMATCH);
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }
}
