package org.telegram.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.config.BotConfig;
import org.telegram.handler.CommandHandler;
import org.telegram.handler.MediaHandler;
import org.telegram.handler.CaptionHandler;
import org.telegram.model.UserSession;
import org.telegram.service.UserSessionService;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Implementação do bot AlbumMaker
 */
public class AlbumMakerBot extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(AlbumMakerBot.class);

    private final CommandHandler commandHandler;
    private final MediaHandler mediaHandler;
    private final CaptionHandler captionHandler;
    private final UserSessionService sessionService;

    public AlbumMakerBot() {
        this.sessionService = new UserSessionService();
        this.commandHandler = new CommandHandler(this);
        this.mediaHandler = new MediaHandler(this, sessionService);
        this.captionHandler = new CaptionHandler(this, sessionService);
    }

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
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                long chatId = message.getChatId();

                // Garantir que há uma sessão de usuário
                UserSession session = sessionService.getOrCreateSession(chatId);

                // Processar comandos
                if (message.hasText() && message.getText().startsWith("/")) {
                    commandHandler.handleCommand(message);
                    return;
                }

                // Processar mensagens com mídia
                if (hasMedia(message)) {
                    mediaHandler.handleMedia(message);
                    return;
                }

                // Processar respostas para adicionar legendas
                if (message.hasText() && message.isReply()) {
                    captionHandler.handleCaption(message);
                    return;
                }

                // Mensagem padrão se não for nenhum dos casos acima
                sendMessage(chatId, "Envie arquivos de mídia (fotos, vídeos) para criar um álbum ou use /help para ver os comandos disponíveis.");
            }
        } catch (Exception e) {
            logger.error("Erro ao processar update", e);
        }
    }

    /**
     * Verifica se a mensagem contém algum tipo de mídia
     */
    private boolean hasMedia(Message message) {
        return message.hasPhoto() ||
                message.hasVideo() ||
                message.hasDocument() ||
                message.hasAnimation();
    }

    /**
     * Método auxiliar para enviar mensagens
     */
    public void sendMessage(long chatId, String texto) {
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