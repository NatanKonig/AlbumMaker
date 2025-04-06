package org.telegram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.bot.AlbumMakerBot;
import org.telegram.model.Album;
import org.telegram.model.MediaItem;
import org.telegram.model.UserSession;
import org.telegram.service.UserSessionService;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaDocument;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaAnimation;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manipulador de mídia
 */
public class MediaHandler {
    private static final Logger logger = LoggerFactory.getLogger(MediaHandler.class);
    private static final int AUTO_ALBUM_DELAY_SECONDS = 5;

    private final AlbumMakerBot bot;
    private final UserSessionService sessionService;
    private final ScheduledExecutorService scheduler;

    public MediaHandler(AlbumMakerBot bot, UserSessionService sessionService) {
        this.bot = bot;
        this.sessionService = sessionService;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Processa mensagens com mídia
     */
    public void handleMedia(Message message) {
        long chatId = message.getChatId();
        UserSession session = sessionService.getOrCreateSession(chatId);

        // Extrair o item de mídia da mensagem
        MediaItem mediaItem = extractMediaItem(message);
        if (mediaItem == null) {
            bot.sendMessage(chatId, "❌ Desculpe, não consegui processar este tipo de mídia.");
            return;
        }

        // Adicionar a mídia à sessão
        session.addMedia(mediaItem);
        logger.info("Mídia adicionada para o chat ID: {}. Total: {}",
                chatId, session.getPendingMedia().size());

        // Se for a primeira mídia, mostrar mensagem informativa
        if (session.getPendingMedia().size() == 1) {
            bot.sendMessage(chatId,
                    "📤 Recebi sua primeira mídia! Envie mais para criar um álbum, " +
                            "ou aguarde alguns segundos para criar um álbum com esta única mídia.");

            // Agendar a criação automática do álbum após o delay
            scheduleAlbumCreation(chatId);
        }
        // Se já tem várias mídias, apenas confirmar o recebimento
        else if (session.getPendingMedia().size() > 1) {
            // Cancelar qualquer criação agendada anteriormente
            cancelScheduledAlbumCreation(chatId);

            bot.sendMessage(chatId,
                    "📤 Mídia adicionada! Contagem atual: " + session.getPendingMedia().size() +
                            "\nEnvie mais mídias ou aguarde alguns segundos para criar o álbum automaticamente.");

            // Agendar a criação automática do álbum após o delay
            scheduleAlbumCreation(chatId);
        }
    }

    /**
     * Extrai o item de mídia de uma mensagem
     */
    private MediaItem extractMediaItem(Message message) {
        // Processar foto
        if (message.hasPhoto()) {
            List<PhotoSize> photos = message.getPhoto();
            // Pegar a foto de maior qualidade (última da lista)
            PhotoSize photo = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);

            if (photo != null) {
                String fileName = "photo_" + photo.getFileId().substring(0, 10) + ".jpg";
                return new MediaItem(photo.getFileId(), fileName, MediaItem.MediaType.PHOTO);
            }
        }

        // Processar vídeo
        if (message.hasVideo()) {
            String fileId = message.getVideo().getFileId();
            String fileName = message.getVideo().getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "video_" + fileId.substring(0, 10) + ".mp4";
            }
            return new MediaItem(fileId, fileName, MediaItem.MediaType.VIDEO);
        }

        // Processar animação (GIF)
        if (message.hasAnimation()) {
            String fileId = message.getAnimation().getFileId();
            String fileName = "animation_" + fileId.substring(0, 10) + ".gif";
            return new MediaItem(fileId, fileName, MediaItem.MediaType.ANIMATION);
        }

        // Processar documento (se for mídia reconhecida)
        if (message.hasDocument()) {
            String fileId = message.getDocument().getFileId();
            String fileName = message.getDocument().getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "document_" + fileId.substring(0, 10);
            }
            return new MediaItem(fileId, fileName, MediaItem.MediaType.DOCUMENT);
        }

        return null;
    }

    /**
     * Agenda a criação automática do álbum
     */
    private void scheduleAlbumCreation(long chatId) {
        // Agendar a tarefa
        scheduler.schedule(() -> {
            try {
                createAlbum(chatId);
            } catch (Exception e) {
                logger.error("Erro ao criar álbum agendado para o chat ID: {}", chatId, e);
                bot.sendMessage(chatId, "❌ Ocorreu um erro ao criar o álbum. Por favor, tente novamente.");
            }
        }, AUTO_ALBUM_DELAY_SECONDS, TimeUnit.SECONDS);

        logger.info("Criação de álbum agendada para o chat ID: {} em {} segundos",
                chatId, AUTO_ALBUM_DELAY_SECONDS);
    }

    /**
     * Cancela a criação agendada do álbum
     */
    private void cancelScheduledAlbumCreation(long chatId) {
        // Como não temos referência direta à tarefa agendada,
        // esta é apenas uma preparação para uma implementação mais robusta
        logger.info("Cancelando criações de álbum agendadas anteriormente para o chat ID: {}", chatId);
    }

    /**
     * Cria um álbum com as mídias pendentes
     */
    public void createAlbum(long chatId) {
        UserSession session = sessionService.getSession(chatId);
        if (session == null || session.getPendingMedia().isEmpty()) {
            logger.warn("Tentativa de criar álbum sem mídias pendentes para o chat ID: {}", chatId);
            return;
        }

        List<MediaItem> mediaItems = session.getPendingMedia();
        logger.info("Criando álbum com {} itens para o chat ID: {}", mediaItems.size(), chatId);

        // Criar o álbum
        Album album = new Album(mediaItems);

        // Enviar o álbum para o usuário
        try {
            List<Message> sentMessages = sendAlbumToUser(chatId, mediaItems);

            // Se o envio foi bem-sucedido, atualizar a sessão
            if (sentMessages != null && !sentMessages.isEmpty()) {
                // Guardamos o ID da mensagem do álbum para poder modificar depois
                album.setSentMessageId(sentMessages.get(0).getMessageId());

                // Atualizar a sessão
                session.setLastAlbum(album);
                session.clearPendingMedia();

                // Informar o usuário que o álbum foi criado
                bot.sendMessage(chatId,
                        "✅ Álbum criado com sucesso! Para adicionar uma legenda, responda ao álbum com o texto desejado.");
            }
        } catch (TelegramApiException e) {
            logger.error("Erro ao enviar álbum para o chat ID: {}", chatId, e);
            bot.sendMessage(chatId,
                    "❌ Ocorreu um erro ao enviar o álbum. Por favor, tente novamente.");
        }
    }

    /**
     * Envia um álbum para o usuário
     */
    private List<Message> sendAlbumToUser(long chatId, List<MediaItem> mediaItems) throws TelegramApiException {
        // Preparar grupo de mídia para envio
        SendMediaGroup mediaGroup = new SendMediaGroup();
        mediaGroup.setChatId(String.valueOf(chatId));

        List<InputMedia> inputMedia = new ArrayList<>();

        // Converter cada item de mídia para o formato do Telegram
        for (MediaItem item : mediaItems) {
            InputMedia media = convertToInputMedia(item);
            if (media != null) {
                inputMedia.add(media);
            }
        }

        // O Telegram permite no máximo 10 itens por álbum
        if (inputMedia.size() > 10) {
            inputMedia = inputMedia.subList(0, 10);
            logger.warn("Álbum truncado para 10 itens para o chat ID: {}", chatId);
        }

        mediaGroup.setMedias(inputMedia);

        // Enviar o grupo de mídia
        return bot.execute(mediaGroup);
    }

    /**
     * Converte um MediaItem para o formato InputMedia do Telegram
     */
    private InputMedia convertToInputMedia(MediaItem item) {
        switch (item.getType()) {
            case PHOTO:
                InputMediaPhoto photo = new InputMediaPhoto();
                photo.setMedia(item.getFileId());
                return photo;

            case VIDEO:
                InputMediaVideo video = new InputMediaVideo();
                video.setMedia(item.getFileId());
                return video;

            case ANIMATION:
                InputMediaAnimation animation = new InputMediaAnimation();
                animation.setMedia(item.getFileId());
                return animation;

            case DOCUMENT:
                InputMediaDocument document = new InputMediaDocument();
                document.setMedia(item.getFileId());
                return document;

            default:
                logger.warn("Tipo de mídia não suportado: {}", item.getType());
                return null;
        }
    }
}