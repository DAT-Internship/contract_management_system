package com.cms.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Persists OVERDUE status for unpaid invoices after their due date. The
 * payment read endpoints also derive overdue state on the fly, so the sweep is
 * mainly for reporting and for keeping stored status consistent.
 */
@Component
public class PaymentOverdueScheduler {

    private static final Logger log = LoggerFactory.getLogger(PaymentOverdueScheduler.class);

    private final PaymentService paymentService;

    public PaymentOverdueScheduler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Scheduled(cron = "${app.payment.overdue-cron:0 5 0 * * *}")
    public void markOverdueInvoices() {
        int updated = paymentService.markOverdueInvoices();
        if (updated > 0) {
            log.info("Payment overdue sweep marked {} invoice(s) as overdue", updated);
        }
    }
}
