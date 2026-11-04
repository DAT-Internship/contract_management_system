package com.cms.demo.service;

import com.cms.demo.entity.Invoice;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;


public final class PaymentCalculator {

    public static final String PENDING = "PENDING";
    public static final String PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String PAID = "PAID";
    public static final String OVERDUE = "OVERDUE";
    public static final String NO_INVOICES = "NO_INVOICES";
    public static final int DEFAULT_PAYMENT_TERM_DAYS = 30;

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private PaymentCalculator() {
    }

    public static String deriveInvoiceStatus(BigDecimal totalAmount, BigDecimal paidAmount,
            LocalDate dueDate, LocalDate today) {
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;

        if (isFullyPaid(total, paid)) {
            return PAID;
        }
        if (dueDate != null && today != null && dueDate.isBefore(today)) {
            return OVERDUE;
        }
        if (paid.signum() > 0) {
            return PARTIALLY_PAID;
        }
        return PENDING;
    }

    public static String deriveContractStatus(Collection<Invoice> invoices, BigDecimal contractValue,
            LocalDate today) {
        if (invoices == null || invoices.isEmpty()) {
            return NO_INVOICES;
        }

        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalValue = contractValue == null ? BigDecimal.ZERO : contractValue;
        boolean allPaid = true;

        for (Invoice invoice : invoices) {
            BigDecimal total = invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount();
            BigDecimal paid = invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
            totalPaid = totalPaid.add(paid);

            boolean invoicePaid = isFullyPaid(total, paid);
            boolean invoiceOverdue = !invoicePaid
                    && invoice.getDueDate() != null
                    && today != null
                    && invoice.getDueDate().isBefore(today);

            if (invoiceOverdue) {
                return OVERDUE;
            }
            if (!invoicePaid) {
                allPaid = false;
            }
        }

        if (allPaid && totalPaid.compareTo(totalValue) >= 0) {
            return PAID;
        }
        if (totalPaid.signum() > 0) {
            return PARTIALLY_PAID;
        }
        return PENDING;
    }

    public static boolean isFullyPaid(BigDecimal totalAmount, BigDecimal paidAmount) {
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        return paid.compareTo(total) >= 0;
    }

    public static BigDecimal outstanding(BigDecimal totalAmount, BigDecimal paidAmount) {
        BigDecimal total = totalAmount == null ? BigDecimal.ZERO : totalAmount;
        BigDecimal paid = paidAmount == null ? BigDecimal.ZERO : paidAmount;
        BigDecimal outstanding = total.subtract(paid);
        return outstanding.signum() < 0 ? money(BigDecimal.ZERO) : money(outstanding);
    }

    public static BigDecimal grossWithTax(BigDecimal value, BigDecimal taxRate) {
        BigDecimal base = value == null ? BigDecimal.ZERO : value;
        BigDecimal rate = taxRate == null ? BigDecimal.ZERO : taxRate;
        BigDecimal tax = base.multiply(rate).divide(HUNDRED, 6, RoundingMode.HALF_UP);
        return money(base.add(tax));
    }

    public static int progressPercent(BigDecimal totalPaid, BigDecimal contractGross) {
        if (totalPaid == null || contractGross == null || contractGross.signum() <= 0) {
            return 0;
        }
        int percent = totalPaid.multiply(HUNDRED)
                .divide(contractGross, 0, RoundingMode.HALF_UP)
                .intValue();
        return Math.max(0, Math.min(100, percent));
    }

    public static BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
