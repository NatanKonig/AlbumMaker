package org.telegram.config;

/**
 * Classe de configuração para armazenar credenciais e configurações do bot
 */
public class BotConfig {
    // Substitua pelas credenciais que você obterá do BotFather
    private static final String BOT_USERNAME = "";
    private static final String BOT_TOKEN = "";

    public static String getBotUsername() {
        return BOT_USERNAME;
    }

    public static String getBotToken() {
        return BOT_TOKEN;
    }
}