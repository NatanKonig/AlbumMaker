package org.telegram.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Classe que representa um álbum de mídia
 */
public class Album {
    private String albumId;
    private List<MediaItem> mediaItems;
    private String caption;
    private LocalDateTime createdAt;
    private Integer sentMessageId; // ID da mensagem do álbum enviado

    public Album() {
        this.albumId = UUID.randomUUID().toString().substring(0, 8);
        this.mediaItems = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public Album(List<MediaItem> mediaItems) {
        this();
        this.mediaItems.addAll(mediaItems);
    }

    public String getAlbumId() {
        return albumId;
    }

    public List<MediaItem> getMediaItems() {
        return mediaItems;
    }

    public void addMediaItem(MediaItem item) {
        mediaItems.add(item);
    }

    public void setMediaItems(List<MediaItem> mediaItems) {
        this.mediaItems = mediaItems;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getSentMessageId() {
        return sentMessageId;
    }

    public void setSentMessageId(Integer sentMessageId) {
        this.sentMessageId = sentMessageId;
    }

    public int size() {
        return mediaItems.size();
    }

    public boolean isEmpty() {
        return mediaItems.isEmpty();
    }

    @Override
    public String toString() {
        return "Album{" +
                "albumId='" + albumId + '\'' +
                ", items=" + mediaItems.size() +
                ", createdAt=" + createdAt +
                '}';
    }
}