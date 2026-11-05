package com.cms.demo.service;

import com.cms.demo.event.ContractUpdatedEvent;
import com.cms.demo.repository.ContractRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ContractUpdatedNotificationListener {
    private static final Logger log = LoggerFactory.getLogger(ContractUpdatedNotificationListener.class);

    private final ContractRepository contractRepository;
    private final ContractReminderService reminderService;

    public ContractUpdatedNotificationListener(
            ContractRepository contractRepository,
            ContractReminderService reminderService) {
        this.contractRepository = contractRepository;
        this.reminderService = reminderService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onContractUpdated(ContractUpdatedEvent event) {
        if (!event.terminated()) {
            return;
        }
        var contract = contractRepository.findNotificationDetailsByContractId(event.contractId())
                .orElseThrow(() -> new IllegalStateException(
                        "Updated contract " + event.contractId() + " could not be reloaded"));
        int notifications = reminderService.notifyContractTerminated(contract, event.actorEmployeeCode());
        if (notifications == 0) {
            log.warn("Contract {} termination generated no stakeholder notifications", event.contractId());
        } else {
            log.info("Created contract termination notifications and email attempts for {} stakeholder(s)",
                    notifications);
        }
    }
}
