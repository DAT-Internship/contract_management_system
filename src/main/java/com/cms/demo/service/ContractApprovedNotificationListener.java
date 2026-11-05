package com.cms.demo.service;

import com.cms.demo.event.ContractApprovedEvent;
import com.cms.demo.repository.ContractRepository;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Component
public class ContractApprovedNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(ContractApprovedNotificationListener.class);

    private final ContractRepository contractRepository;
    private final ContractReminderService reminderService;

    public ContractApprovedNotificationListener(
            ContractRepository contractRepository,
            ContractReminderService reminderService) {
        this.contractRepository = contractRepository;
        this.reminderService = reminderService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onContractApproved(ContractApprovedEvent event) {
        var contract = contractRepository.findNotificationDetailsByContractId(event.contractId())
                .orElseThrow(() -> new IllegalStateException(
                        "Approved contract " + event.contractId() + " could not be reloaded"));
        int notifications = reminderService.notifyContractApproved(contract, LocalDate.now());
        if (notifications == 0) {
            log.warn("Approval notification for contract {} was not created for any recipients",
                    event.contractId());
        } else {
            log.info("Created approval notifications and email attempts for {} recipient(s) for contract {}",
                    notifications, event.contractId());
        }
    }
}
