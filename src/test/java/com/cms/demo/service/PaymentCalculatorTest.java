package com.cms.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cms.demo.entity.Invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);

    @Test
    void invoiceIsPaidWhenCumulativePaymentCoversTotal() {
        assertEquals(PaymentCalculator.PAID,
                PaymentCalculator.deriveInvoiceStatus(
                        new BigDecimal("100.00"), new BigDecimal("100.00"),
                        TODAY.plusDays(5), TODAY));
    }

    @Test
    void invoiceIsOverdueWhenPastDueAndNotFullyPaid() {
        assertEquals(PaymentCalculator.OVERDUE,
                PaymentCalculator.deriveInvoiceStatus(
                        new BigDecimal("100.00"), new BigDecimal("40.00"),
                        TODAY.minusDays(1), TODAY));
    }

    @Test
    void invoiceIsPartiallyPaidBeforeDueDate() {
        assertEquals(PaymentCalculator.PARTIALLY_PAID,
                PaymentCalculator.deriveInvoiceStatus(
                        new BigDecimal("100.00"), new BigDecimal("40.00"),
                        TODAY.plusDays(1), TODAY));
    }

    @Test
    void invoiceIsPendingWithoutPaymentsBeforeDueDate() {
        assertEquals(PaymentCalculator.PENDING,
                PaymentCalculator.deriveInvoiceStatus(
                        new BigDecimal("100.00"), BigDecimal.ZERO,
                        TODAY, TODAY));
    }

    @Test
    void contractWithoutInvoicesHasNoInvoicesStatus() {
        assertEquals(PaymentCalculator.NO_INVOICES,
                PaymentCalculator.deriveContractStatus(List.of(), new BigDecimal("100.00"), TODAY));
    }

    @Test
    void contractIsPaidWhenEveryInvoiceIsSettledAndValueIsCovered() {
        Invoice invoice = invoice("100.00", "100.00", TODAY.plusDays(3));

        assertEquals(PaymentCalculator.PAID,
                PaymentCalculator.deriveContractStatus(List.of(invoice), new BigDecimal("100.00"), TODAY));
    }

    @Test
    void contractIsPartiallyPaidWhenOnlySomeInvoicesAreSettled() {
        Invoice paid = invoice("50.00", "50.00", TODAY.plusDays(3));
        Invoice open = invoice("50.00", "0.00", TODAY.plusDays(10));

        assertEquals(PaymentCalculator.PARTIALLY_PAID,
                PaymentCalculator.deriveContractStatus(List.of(paid, open), new BigDecimal("100.00"), TODAY));
    }

    @Test
    void contractIsOverdueWhenAnyUnpaidInvoicePassedDueDate() {
        Invoice overdue = invoice("50.00", "0.00", TODAY.minusDays(2));
        Invoice open = invoice("50.00", "0.00", TODAY.plusDays(10));

        assertEquals(PaymentCalculator.OVERDUE,
                PaymentCalculator.deriveContractStatus(List.of(overdue, open), new BigDecimal("100.00"), TODAY));
    }

    @Test
    void contractIsPartiallyPaidWhenInvoicesAreSettledButValueIsNotCovered() {
        Invoice invoice = invoice("50.00", "50.00", TODAY.plusDays(3));

        assertEquals(PaymentCalculator.PARTIALLY_PAID,
                PaymentCalculator.deriveContractStatus(List.of(invoice), new BigDecimal("100.00"), TODAY));
    }

    @Test
    void contractIsNotPaidWhenGrossIsNotCoveredEvenIfNetValueIs() {
        Invoice invoice = invoice("100.00", "100.00", TODAY.plusDays(3));

        assertEquals(PaymentCalculator.PARTIALLY_PAID,
                PaymentCalculator.deriveContractStatus(List.of(invoice), new BigDecimal("105.00"), TODAY));
    }

    @Test
    void remainingNeverGoesNegative() {
        assertEquals(new BigDecimal("2.50"),
                PaymentCalculator.remaining(new BigDecimal("10.00"), new BigDecimal("7.50")));
        assertEquals(new BigDecimal("0.00"),
                PaymentCalculator.remaining(new BigDecimal("10.00"), new BigDecimal("15.00")));
    }

    @Test
    void outstandingNeverGoesNegative() {
        assertEquals(new BigDecimal("0.00"),
                PaymentCalculator.outstanding(new BigDecimal("10.00"), new BigDecimal("15.00")));
        assertEquals(new BigDecimal("2.50"),
                PaymentCalculator.outstanding(new BigDecimal("10.00"), new BigDecimal("7.50")));
    }

    @Test
    void progressIsCappedAtOneHundredPercent() {
        assertEquals(100, PaymentCalculator.progressPercent(
                new BigDecimal("120.00"), new BigDecimal("100.00")));
        assertEquals(50, PaymentCalculator.progressPercent(
                new BigDecimal("50.00"), new BigDecimal("100.00")));
    }

    @Test
    void grossWithTaxUsesContractTaxRate() {
        assertEquals(new BigDecimal("105.00"),
                PaymentCalculator.grossWithTax(new BigDecimal("100.00"), new BigDecimal("5.00")));
    }

    private Invoice invoice(String total, String paid, LocalDate dueDate) {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(new BigDecimal(total));
        invoice.setPaidAmount(new BigDecimal(paid));
        invoice.setDueDate(dueDate);
        return invoice;
    }
}
