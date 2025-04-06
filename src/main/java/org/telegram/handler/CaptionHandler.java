package org.telegram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.bot.AlbumMakerBot;
import org.telegram.model.Album;
import org.telegram.model.UserSession;
import org.telegram.service.UserSessionService;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageCaption;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Manipulador de legendas para álbuns
 */
public class CaptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(CaptionHandler.class);

    private final AlbumMakerBot bot;
    private final UserSessionService sessionService;

    public CaptionHandler(AlbumMakerBot bot, UserSessionService sessionService) {
        this.bot = bot;
        this.sessionService = sessionService;
    }

    /**
     * Processa mensagens de resposta para adicionar ou modificar legendas
     */
    public void handleCaption(Message message) {
        if (!message.isReply() || !message.hasText()) {
            return;
        }

        long chatId = message.getChatId();
        String caption = message.getText();
        Integer replyToMessageId = message.getReplyToMessage().getMessageId();

        logger.info("Recebida resposta para adicionar/modificar legenda do chat ID: {}", chatId);

        // Verificar se o usuário está respondendo ao álbum dele
        UserSession session = sessionService.getSession(chatId);
        if (session == null || session.getLastAlbum() == null) {
            bot.sendMessage(chatId, "❌ Não encontrei nenhum álbum recente para adicionar a legenda.");
            return;
        }

        Album album = session.getLastAlbum();

        // Verificar se a resposta é para o álbum correto
        if (!replyToMessageId.equals(album.getSentMessageId())) {
            logger.info("A resposta não é para o último álbum enviado. Reply to: {}, Album ID: {}",
                    replyToMessageId, album.getSentMessageId());
            bot.sendMessage(chatId, "❌ Por favor, responda diretamente ao álbum para adicionar uma legenda.");
            return;
        }

        // Atualizar a legenda do álbum
        album.setCaption(caption);

        // Tentar atualizar a legenda na mensagem do Telegram
        try {
            updateAlbumCaption(chatId, album);
            bot.sendMessage(chatId, "✅ Legenda adicionada com sucesso!");
        } catch (TelegramApiException e) {
            logger.error("Erro ao atualizar legenda do álbum para o chat ID: {}", chatId, e);
            bot.sendMessage(chatId, "❌ Ocorreu um erro ao adicionar a legenda. Por favor, tente novamente.");
        }
    }

    /**
     * Atualiza a legenda de um álbum no Telegram
     */
    private void updateAlbumCaption(long chatId, Album album) throws TelegramApiException {
        // Infelizmente, o Telegram não permite editar a legenda de um grupo de mídia inteiro
        // Só podemos editar a legenda do primeiro item do álbum

        EditMessageCaption editCaption = new EditMessageCaption();
        editCaption.setChatId(String.valueOf(chatId));
        editCaption.setMessageId(album.getSentMessageId());
        editCaption.setCaption(album.getCaption());

        bot.execute(editCaption);
        logger.info("Legenda atualizada com sucesso para o álbum {} do chat ID: {}",
                album.getAlbumId(), chatId);
    }
}