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

            // Registrar o bot
            botsApi.registerBot(new AlbumMakerBot());

            logger.info("AlbumMaker Bot iniciado com sucesso!");
        } catch (TelegramApiException e) {
            logger.error("Erro ao iniciar o AlbumMaker Bot", e);
        }
    }
}