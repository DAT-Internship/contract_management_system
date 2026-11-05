package com.cms.demo.service;

import com.cms.demo.event.InvoiceActivityEvent;
import com.cms.demo.repository.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InvoiceActivityNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(InvoiceActivityNotificationListener.class);

    private final InvoiceRepository invoiceRepository;
    private final ContractReminderService reminderService;

    public InvoiceActivityNotificationListener(
            InvoiceRepository invoiceRepository,
            ContractReminderService reminderService) {
        this.invoiceRepository = invoiceRepository;
        this.reminderService = reminderService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onInvoiceActivity(InvoiceActivityEvent event) {
        var invoice = invoiceRepository.findNotificationDetailsByInvoiceId(event.invoiceId())
                .orElseThrow(() -> new IllegalStateException(
                        "Invoice " + event.invoiceId() + " could not be reloaded for notification"));
        int notifications = reminderService.notifyInvoiceActivity(invoice, event.eventType());
        if (notifications == 0) {
            log.warn("Invoice activity {} for invoice {} produced no recipient notifications",
                    event.eventType(), event.invoiceId());
        } else {
            log.info("Created {} notifications and email attempts for invoice {} activity {}",
                    notifications, event.invoiceId(), event.eventType());
        }
    }
}
