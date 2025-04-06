package org.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.model.UserSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Serviço para gerenciar sessões de usuários
 */
public class UserSessionService {
    private static final Logger logger = LoggerFactory.getLogger(UserSessionService.class);
    private static final int SESSION_TIMEOUT_MINUTES = 30;

    private final Map<Long, UserSession> sessions;
    private final ScheduledExecutorService scheduler;

    public UserSessionService() {
        this.sessions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // Agendar limpeza de sessões expiradas a cada 10 minutos
        scheduler.scheduleAtFixedRate(
                this::cleanExpiredSessions,
                10, 10, TimeUnit.MINUTES);
    }

    /**
     * Obtém a sessão do usuário ou cria uma nova se não existir
     */
    public UserSession getOrCreateSession(long chatId) {
        return sessions.computeIfAbsent(chatId, UserSession::new);
    }

    /**
     * Obtém a sessão do usuário se existir
     */
    public UserSession getSession(long chatId) {
        return sessions.get(chatId);
    }

    /**
     * Remove a sessão do usuário
     */
    public void removeSession(long chatId) {
        sessions.remove(chatId);
        logger.info("Sessão removida para o chat ID: {}", chatId);
    }

    /**
     * Limpa as sessões expiradas
     */
    private void cleanExpiredSessions() {
        logger.info("Iniciando limpeza de sessões expiradas...");

        sessions.entrySet()
                .removeIf(entry -> {
                    boolean expired = entry.getValue().isExpired(SESSION_TIMEOUT_MINUTES);
                    if (expired) {
                        logger.info("Removendo sessão expirada para o chat ID: {}", entry.getKey());
                    }
                    return expired;
                });

        logger.info("Limpeza concluída. Total de sessões ativas: {}", sessions.size());
    }

    /**
     * Finaliza o serviço de sessões
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}