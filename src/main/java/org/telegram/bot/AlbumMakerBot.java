package org.telegram.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.config.BotConfig;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Implementação do bot AlbumMaker
 */
public class AlbumMakerBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(AlbumMakerBot.class);

    @Override
    public String getBotUsername() {
        return BotConfig.getBotUsername();
    }

    @Override
    public String getBotToken() {
        return BotConfig.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        // Verificar se a atualização contém uma mensagem e se a mensagem tem texto
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            logger.info("Recebida mensagem: '{}' do chat ID: {}", messageText, chatId);

            // Processar comandos
            if (messageText.startsWith("/")) {
                processarComando(messageText, chatId);
            } else {
                // Resposta padrão para mensagens que não são comandos
                enviarMensagem(chatId, "Você enviou: " + messageText);
            }
        }
    }

    /**
     * Processa comandos recebidos pelo bot
     */
    private void processarComando(String comando, long chatId) {
        // Extrair o comando (primeira palavra após /)
        String[] partes = comando.split("\\s+");
        String cmd = partes[0].toLowerCase();

        switch (cmd) {
            case "/start":
                enviarMensagem(chatId, "Bem-vindo ao AlbumMaker Bot! Use /help para ver os comandos disponíveis.");
                break;

            case "/help":
                enviarMensagem(chatId,
                        "Comandos disponíveis:\n" +
                                "/start - Iniciar o bot\n" +
                                "/help - Mostrar esta ajuda\n" +
                                "/sobre - Informações sobre o bot"
                );
                break;

            case "/sobre":
                enviarMensagem(chatId, "AlbumMaker Bot v1.0\nUm bot para criar e gerenciar álbuns de mídia no Telegram.");
                break;

            default:
                enviarMensagem(chatId, "Comando não reconhecido. Use /help para ver os comandos disponíveis.");
                break;
        }
    }

    /**
     * Método auxiliar para enviar mensagens
     */
    private void enviarMensagem(long chatId, String texto) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(texto);

        try {
            execute(message);
            logger.info("Mensagem enviada com sucesso para o chat ID: {}", chatId);
        } catch (TelegramApiException e) {
            logger.error("Erro ao enviar mensagem para o chat ID: {}", chatId, e);
        }
    }
}