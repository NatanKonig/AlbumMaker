package org.telegram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.bot.AlbumMakerBot;
import org.telegram.model.Album;
import org.telegram.model.MediaItem;
import org.telegram.model.UserSession;
import org.telegram.service.UserSessionService;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manipulador de mídia com suporte a múltiplos álbuns
 */
public class MediaHandler {
    private static final Logger logger = LoggerFactory.getLogger(MediaHandler.class);
    private static final int AUTO_ALBUM_DELAY_SECONDS = 3;
    private static final int MAX_MEDIA_PER_ALBUM = 10;

    private final AlbumMakerBot bot;
    private final UserSessionService sessionService;
    private final ScheduledExecutorService scheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

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

        // Cancelar qualquer tarefa agendada anteriormente
        cancelScheduledAlbumCreation(chatId);

        // Agendar a criação automática do álbum após o delay
        scheduleAlbumCreation(chatId);
    }

    /**
     * Extrai o item de mídia de uma mensagem
     */
    private MediaItem extractMediaItem(Message message) {
        Integer messageId = message.getMessageId();

        // Processar foto
        if (message.hasPhoto()) {
            List<PhotoSize> photos = message.getPhoto();
            // Pegar a foto de maior qualidade (última da lista)
            PhotoSize photo = photos.stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);

            if (photo != null) {
                String fileName = "photo_" + photo.getFileId().substring(0, 10) + ".jpg";
                return new MediaItem(photo.getFileId(), fileName, MediaItem.MediaType.PHOTO, messageId);
            }
        }

        // Processar vídeo
        if (message.hasVideo()) {
            String fileId = message.getVideo().getFileId();
            String fileName = message.getVideo().getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "video_" + fileId.substring(0, 10) + ".mp4";
            }
            return new MediaItem(fileId, fileName, MediaItem.MediaType.VIDEO, messageId);
        }

        // Processar animação (GIF)
        if (message.hasAnimation()) {
            String fileId = message.getAnimation().getFileId();
            String fileName = "animation_" + fileId.substring(0, 10) + ".gif";
            return new MediaItem(fileId, fileName, MediaItem.MediaType.ANIMATION, messageId);
        }

        // Processar documento (se for mídia reconhecida)
        if (message.hasDocument()) {
            String fileId = message.getDocument().getFileId();
            String fileName = message.getDocument().getFileName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "document_" + fileId.substring(0, 10);
            }
            return new MediaItem(fileId, fileName, MediaItem.MediaType.DOCUMENT, messageId);
        }

        return null;
    }

    /**
     * Agenda a criação automática do álbum
     */
    private void scheduleAlbumCreation(long chatId) {
        // Agendar a tarefa
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                createAlbums(chatId);
            } catch (Exception e) {
                logger.error("Erro ao criar álbum agendado para o chat ID: {}", chatId, e);
                bot.sendMessage(chatId, "❌ Ocorreu um erro ao criar o álbum. Por favor, tente novamente.");
            } finally {
                // Remover a tarefa da lista após execução
                scheduledTasks.remove(chatId);
            }
        }, AUTO_ALBUM_DELAY_SECONDS, TimeUnit.SECONDS);

        // Salvar a referência à tarefa
        scheduledTasks.put(chatId, future);

        logger.info("Criação de álbum agendada para o chat ID: {} em {} segundos",
                chatId, AUTO_ALBUM_DELAY_SECONDS);
    }

    /**
     * Cancela a criação agendada do álbum
     */
    private void cancelScheduledAlbumCreation(long chatId) {
        ScheduledFuture<?> task = scheduledTasks.get(chatId);
        if (task != null && !task.isDone()) {
            task.cancel(false);
            scheduledTasks.remove(chatId);
            logger.info("Tarefa de criação de álbum cancelada para o chat ID: {}", chatId);
        }
    }

    /**
     * Cria múltiplos álbuns se necessário para todas as mídias pendentes
     */
    public void createAlbums(long chatId) {
        UserSession session = sessionService.getSession(chatId);
        if (session == null || session.getPendingMedia().isEmpty()) {
            logger.warn("Tentativa de criar álbum sem mídias pendentes para o chat ID: {}", chatId);
            return;
        }

        List<MediaItem> mediaItems = new ArrayList<>(session.getPendingMedia());
        int totalMedias = mediaItems.size();
        logger.info("Criando álbuns com {} itens totais para o chat ID: {}", totalMedias, chatId);

        // Se houver apenas uma mídia, enviar mensagem informativa
        if (totalMedias == 1) {
            bot.sendMessage(chatId,
                    "ℹ️ Para criar um álbum, você precisa enviar pelo menos 2 mídias. " +
                            "Envie mais mídias e tente novamente.");
            return;
        }

        // Limpar a sessão antes de enviar os álbuns para evitar duplicações
        List<MediaItem> allMediaItems = new ArrayList<>(session.getPendingMedia());
        session.clearPendingMedia();

        // Criar múltiplos álbuns se necessário
        int albumCount = (int) Math.ceil((double) totalMedias / MAX_MEDIA_PER_ALBUM);
        logger.info("Criando {} álbuns para o chat ID: {}", albumCount, chatId);

        boolean success = true;

        for (int i = 0; i < albumCount; i++) {
            int fromIndex = i * MAX_MEDIA_PER_ALBUM;
            int toIndex = Math.min(fromIndex + MAX_MEDIA_PER_ALBUM, totalMedias);

            List<MediaItem> albumItems = allMediaItems.subList(fromIndex, toIndex);
            if (albumItems.size() >= 2) {  // O Telegram exige pelo menos 2 itens por álbum
                boolean albumSuccess = createSingleAlbum(chatId, albumItems, i + 1, albumCount);
                if (!albumSuccess) {
                    success = false;
                }
            } else {
                // Se sobrarem itens que não são suficientes para um álbum, avise o usuário
                logger.warn("Itens insuficientes ({}) para criar um álbum para o chat ID: {}",
                        albumItems.size(), chatId);
                if (i == 0) {  // Se for o primeiro e único álbum
                    bot.sendMessage(chatId,
                            "ℹ️ Para criar um álbum, você precisa enviar pelo menos 2 mídias. " +
                                    "Envie mais mídias e tente novamente.");
                }
                success = false;
            }
        }

        // Se os álbuns foram criados com sucesso, delete as mensagens originais
        if (success) {
            deleteOriginalMessages(chatId, allMediaItems);
        }
    }

    /**
     * Deleta as mensagens originais após a criação bem-sucedida dos álbuns
     */
    private void deleteOriginalMessages(long chatId, List<MediaItem> mediaItems) {
        // Agendar a exclusão para ocorrer após um pequeno delay para garantir que o álbum foi exibido
        scheduler.schedule(() -> {
            int deletedCount = 0;

            for (MediaItem item : mediaItems) {
                Integer messageId = item.getMessageId();
                if (messageId != null) {
                    try {
                        DeleteMessage deleteMessage = new DeleteMessage();
                        deleteMessage.setChatId(String.valueOf(chatId));
                        deleteMessage.setMessageId(messageId);

                        boolean deleted = bot.execute(deleteMessage);
                        if (deleted) {
                            deletedCount++;
                        }
                    } catch (TelegramApiException e) {
                        logger.warn("Não foi possível deletar a mensagem ID: {} para o chat ID: {}",
                                messageId, chatId, e);
                    }
                }
            }

            logger.info("Deletadas {} de {} mensagens originais para o chat ID: {}",
                    deletedCount, mediaItems.size(), chatId);
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * Cria um único álbum com as mídias especificadas
     * @return true se o álbum foi criado com sucesso, false caso contrário
     */
    private boolean createSingleAlbum(long chatId, List<MediaItem> mediaItems, int albumNumber, int totalAlbums) {
        logger.info("Criando álbum {}/{} com {} itens para o chat ID: {}",
                albumNumber, totalAlbums, mediaItems.size(), chatId);

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
                UserSession session = sessionService.getSession(chatId);
                if (session != null) {
                    session.setLastAlbum(album);

                    // Informar o usuário apenas uma vez, após o último álbum
                    if (albumNumber == totalAlbums) {
                        String message = totalAlbums > 1
                                ? String.format("✅ Criados %d álbuns com sucesso! Para adicionar uma legenda, responda a um álbum com o texto desejado.", totalAlbums)
                                : "✅ Álbum criado com sucesso! Para adicionar uma legenda, responda ao álbum com o texto desejado.";
                        bot.sendMessage(chatId, message);
                    }
                }

                return true;
            }

            return false;
        } catch (TelegramApiException e) {
            logger.error("Erro ao enviar álbum {}/{} para o chat ID: {}",
                    albumNumber, totalAlbums, chatId, e);

            // Informar o usuário apenas uma vez em caso de erro
            if (albumNumber == 1) {
                bot.sendMessage(chatId,
                        "❌ Ocorreu um erro ao enviar o álbum. Por favor, tente novamente.");
            }

            return false;
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

        // Verificar se temos pelo menos 2 itens para o álbum (requisito do Telegram)
        if (inputMedia.size() < 2) {
            throw new TelegramApiException("Número insuficiente de itens de mídia para criar um álbum");
        }

        // O Telegram permite no máximo 10 itens por álbum
        if (inputMedia.size() > MAX_MEDIA_PER_ALBUM) {
            inputMedia = inputMedia.subList(0, MAX_MEDIA_PER_ALBUM);
            logger.warn("Álbum truncado para {} itens para o chat ID: {}", MAX_MEDIA_PER_ALBUM, chatId);
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