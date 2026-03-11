package com.example.project.domain.dispatch.domain.dispatchEnum;

public final class DispatchEnums {

    private DispatchEnums() {
    }

    public enum DispatchJobType {
        INITIAL,
        REASSIGN,
        RESCUE
    }

    public enum DispatchJobStatus {
        QUEUED,
        SEARCHING,
        OFFERING,
        WAITING_RESPONSE,
        MATCHED,
        FAILED,
        CANCELLED;

        public boolean isTerminal() {
            return this == MATCHED || this == FAILED || this == CANCELLED;
        }
    }

    public enum DispatchMode {
        MANUAL,
        AUTO_OFFER,
        AUTO_ASSIGN
    }

    public enum DispatchPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }

    public enum DispatchOfferStatus {
        PENDING,
        PUSH_SENT,
        OPENED,
        ACCEPTED,
        REJECTED,
        EXPIRED,
        CANCELLED;

        public boolean isTerminal() {
            return this == ACCEPTED || this == REJECTED || this == EXPIRED || this == CANCELLED;
        }
    }

    public enum DriverAvailabilityStatus {
        ONLINE,
        OFFLINE,
        BUSY,
        RESTING,
        BLOCKED
    }
}
