package ru.practicum.main_service.event.model;

public enum StateAction {
    SEND_TO_REVIEW, CANCEL_REVIEW, PUBLISH_EVENT, REJECT_EVENT;

    public boolean isAdminStateAction() {
        return this == PUBLISH_EVENT || this == REJECT_EVENT;
    }

    public boolean isUserStateAction() {
        return this == SEND_TO_REVIEW || this == CANCEL_REVIEW;
    }
}
