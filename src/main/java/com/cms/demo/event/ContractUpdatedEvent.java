package com.cms.demo.event;

public record ContractUpdatedEvent(Long contractId, String actorEmployeeCode, boolean terminated) {
}
