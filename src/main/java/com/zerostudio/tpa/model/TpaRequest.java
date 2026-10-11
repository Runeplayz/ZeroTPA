package com.zerostudio.tpa.model;

import java.util.UUID;

public record TpaRequest(UUID senderId, UUID targetId, String senderName, String targetName, Type type, long createdAt) {
    public enum Type {
        TPA,
        TPA_HERE
    }
}