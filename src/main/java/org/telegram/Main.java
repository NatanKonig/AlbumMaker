package org.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.bot.AlbumMakerBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Classe principal para iniciar o bot AlbumMaker
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Iniciando o AlbumMaker Bot...");

        try {
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
        } catch (TelegramApiException e) {
            logger.error("Erro ao iniciar o AlbumMaker Bot", e);
        }
    }
}