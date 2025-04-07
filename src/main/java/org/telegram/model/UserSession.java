package org.telegram.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classe que representa uma sessão de usuário
 */
public class UserSession {
    private final long chatId;
    private List<MediaItem> pendingMedia;
    private Album lastAlbum;
    private LocalDateTime lastActivity;
    private SessionState state;

    public enum SessionState {
        IDLE,               // Estado inicial
        COLLECTING_MEDIA,   // Coletando mídia para o álbum
        WAITING_FOR_CAPTION // Esperando legenda para o álbum
    }

    public UserSession(long chatId) {
        this.chatId = chatId;
        this.pendingMedia = new ArrayList<>();
        this.lastActivity = LocalDateTime.now();
        this.state = SessionState.IDLE;
    }

    public long getChatId() {
        return chatId;
    }

    public List<MediaItem> getPendingMedia() {
        return Collections.unmodifiableList(pendingMedia);
    }

    public void addMedia(MediaItem media) {
        if (pendingMedia == null) {
            pendingMedia = new ArrayList<>();
        }
        pendingMedia.add(media);
        setState(SessionState.COLLECTING_MEDIA);
        updateLastActivity();
    }

    public void clearPendingMedia() {
        if (pendingMedia != null) {
            pendingMedia.clear();
        }
    }

    public Album getLastAlbum() {
        return lastAlbum;
    }

    public void setLastAlbum(Album album) {
        this.lastAlbum = album;
        setState(SessionState.WAITING_FOR_CAPTION);
        updateLastActivity();
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
    }

    public SessionState getState() {
        return state;
    }

    public void setState(SessionState state) {
        this.state = state;
        updateLastActivity();
    }

    public boolean hasEnoughMediaForAlbum() {
        return pendingMedia != null && pendingMedia.size() >= 2;
    }

    public boolean isExpired(int timeoutMinutes) {
        return LocalDateTime.now().isAfter(lastActivity.plusMinutes(timeoutMinutes));
    }
}