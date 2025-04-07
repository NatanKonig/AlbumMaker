package org.telegram.model;

import java.time.LocalDateTime;

/**
 * Classe que representa um item de mídia
 */
public class MediaItem {
    private String fileId;
    private String fileName;
    private MediaType type;
    private LocalDateTime receivedAt;
    private String uniqueId;
    private Integer messageId; // ID da mensagem original

    public enum MediaType {
        PHOTO,
        VIDEO,
        DOCUMENT,
        ANIMATION
    }

    public MediaItem(String fileId, String fileName, MediaType type, Integer messageId) {
        this.fileId = fileId;
        this.fileName = fileName;
        this.type = type;
        this.messageId = messageId;
        this.receivedAt = LocalDateTime.now();
        this.uniqueId = generateUniqueId();
    }

    private String generateUniqueId() {
        return fileId.substring(0, Math.min(fileId.length(), 10)) +
                "-" +
                System.currentTimeMillis();
    }

    public String getFileId() {
        return fileId;
    }

    public String getFileName() {
        return fileName;
    }

    public MediaType getType() {
        return type;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public Integer getMessageId() {
        return messageId;
    }

    @Override
    public String toString() {
        return "MediaItem{" +
                "type=" + type +
                ", fileName='" + fileName + '\'' +
                ", messageId=" + messageId +
                ", receivedAt=" + receivedAt +
                '}';
    }
}