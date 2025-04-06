package org.telegram.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.bot.AlbumMakerBot;
import org.telegram.telegrambots.meta.api.objects.Message;

/**
 * Manipulador de comandos do bot
 */
public class CommandHandler {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandler.class);
    private final AlbumMakerBot bot;

    public CommandHandler(AlbumMakerBot bot) {
        this.bot = bot;
    }

    /**
     * Processa os comandos recebidos
     */
    public void handleCommand(Message message) {
        String text = message.getText();
        long chatId = message.getChatId();
        String username = message.getFrom().getUserName();

        logger.info("Comando recebido: {} do usuário: {}", text, username);

        // Extrair o comando (primeira palavra após /)
        String[] parts = text.split("\\s+");
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/start":
                handleStart(chatId);
                break;

            case "/help":
                handleHelp(chatId);
                break;

            case "/cancel":
                handleCancel(chatId);
                break;

            case "/about":
            case "/sobre":
                handleAbout(chatId);
                break;

            default:
                bot.sendMessage(chatId, "Comando não reconhecido. Use /help para ver os comandos disponíveis.");
                break;
        }
    }

    private void handleStart(long chatId) {
        String welcomeMessage = "👋 Bem-vindo ao AlbumMaker Bot!\n\n" +
                "Este bot permite que você crie álbuns com suas mídias de forma fácil.\n\n" +
                "Para começar:\n" +
                "1. Envie várias fotos e/ou vídeos\n" +
                "2. Aguarde alguns segundos enquanto o álbum é criado\n" +
                "3. Responda ao álbum com o texto que deseja adicionar como legenda\n\n" +
                "Use /help para ver todos os comandos disponíveis.";

        bot.sendMessage(chatId, welcomeMessage);
    }

    private void handleHelp(long chatId) {
        String helpMessage = "🔍 *Comandos disponíveis:*\n\n" +
                "/start - Iniciar o bot e ver as boas-vindas\n" +
                "/help - Mostrar esta mensagem de ajuda\n" +
                "/cancel - Cancelar a criação do álbum atual\n" +
                "/about - Informações sobre o bot\n\n" +
                "*Como usar:*\n" +
                "1. Envie várias fotos e/ou vídeos\n" +
                "2. Aguarde alguns segundos enquanto o álbum é criado\n" +
                "3. Responda ao álbum com o texto que deseja adicionar como legenda";

        bot.sendMessage(chatId, helpMessage);
    }

    private void handleCancel(long chatId) {
        // Lógica para cancelar a criação do álbum será implementada depois
        bot.sendMessage(chatId, "✅ Operação atual cancelada. Você pode começar um novo álbum enviando mídias.");
    }

    private void handleAbout(long chatId) {
        String aboutMessage = "📱 *AlbumMaker Bot* v1.0\n\n" +
                "Um bot para criar e gerenciar álbuns de mídia no Telegram.\n\n" +
                "Recursos:\n" +
                "• Crie álbuns com fotos e vídeos\n" +
                "• Adicione ou modifique legendas\n" +
                "• Interface simples e intuitiva\n\n" +
                "Desenvolvido com ❤️";

        bot.sendMessage(chatId, aboutMessage);
    }
}