package org.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.bot.AlbumMakerBot;
import org.telegram.config.BotConfig;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetMe;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Classe principal para iniciar o bot AlbumMaker
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Iniciando o AlbumMaker Bot...");

        int maxRetries = 3;
        int retryDelay = 5; // segundos

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.info("Tentativa {} de {} para inicializar o bot", attempt, maxRetries);
                
                // Validar token antes de registrar o bot
                if (!validateBotToken()) {
                    logger.error("Token do bot inválido. Verifique a configuração.");
                    System.exit(1);
                }
                
                // Inicializar a API do Telegram
                TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

                // Criar e registrar o bot
                AlbumMakerBot albumMakerBot = new AlbumMakerBot();
                botsApi.registerBot(albumMakerBot);

                logger.info("AlbumMaker Bot iniciado com sucesso!");

                // Adicionar um gancho de desligamento para limpar recursos
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    logger.info("Desligando o AlbumMaker Bot...");
                    // Adicionar aqui qualquer limpeza necessária
                }));
                
                // Se chegou aqui, foi bem-sucedido
                return;
                
            } catch (TelegramApiException e) {
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                
                // Se é erro de webhook e não é a última tentativa
                if (errorMsg.contains("webhook") && attempt < maxRetries) {
                    logger.warn("Erro de webhook na tentativa {}/{}: {}. Tentando novamente em {} segundos...", 
                              attempt, maxRetries, e.getMessage(), retryDelay);
                    
                    try {
                        Thread.sleep(retryDelay * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        logger.error("Interrompido durante a espera entre tentativas");
                        return;
                    }
                } else {
                    // Erro final ou não relacionado a webhook
                    logger.error("Erro ao iniciar o AlbumMaker Bot na tentativa {}/{}", attempt, maxRetries, e);
                    
                    if (attempt == maxRetries) {
                        logger.error("Todas as tentativas falharam. Encerrando aplicação.");
                        System.exit(1);
                    }
                }
            }
        }
    }
    
    /**
     * Valida se o token do bot está funcionando
     */
    private static boolean validateBotToken() {
        try {
            AlbumMakerBot tempBot = new AlbumMakerBot();
            GetMe getMe = new GetMe();
            User botUser = tempBot.execute(getMe);
            
            if (botUser != null) {
                logger.info("Token validado com sucesso. Bot: {} (@{})", 
                          botUser.getFirstName(), botUser.getUserName());
                return true;
            }
            
            return false;
            
        } catch (TelegramApiException e) {
            logger.error("Falha na validação do token: {}", e.getMessage());
            return false;
        }
    }
}