package org.telegram.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Classe de configuração para armazenar credenciais e configurações do bot
 */
public class BotConfig {
    private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);
    private static final Properties properties = new Properties();

    private static String BOT_USERNAME;
    private static String BOT_TOKEN;
    private static Long BACKUP_GROUP_ID;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = BotConfig.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                logger.error("Não foi possível encontrar o arquivo config.properties");
                System.exit(1);
            }

            // Carregar as propriedades
            properties.load(input);

            // Obter os valores de configuração
            BOT_USERNAME = properties.getProperty("bot.username");
            BOT_TOKEN = properties.getProperty("bot.token");
            
            // Obter o ID do grupo de backup (opcional)
            String backupGroupIdStr = properties.getProperty("backup.group.id");
            if (backupGroupIdStr != null && !backupGroupIdStr.trim().isEmpty()) {
                try {
                    BACKUP_GROUP_ID = Long.parseLong(backupGroupIdStr.trim());
                    logger.info("Grupo de backup configurado: {}", BACKUP_GROUP_ID);
                } catch (NumberFormatException e) {
                    logger.warn("ID do grupo de backup inválido: {}. Funcionalidade de backup desabilitada.", backupGroupIdStr);
                    BACKUP_GROUP_ID = null;
                }
            } else {
                logger.info("Grupo de backup não configurado. Funcionalidade de backup desabilitada.");
                BACKUP_GROUP_ID = null;
            }

            if (BOT_USERNAME == null || BOT_TOKEN == null) {
                logger.error("Credenciais do bot não encontradas no arquivo de configuração");
                System.exit(1);
            }

            logger.info("Configurações carregadas com sucesso");
        } catch (IOException e) {
            logger.error("Erro ao carregar o arquivo de configuração", e);
            System.exit(1);
        }
    }

    public static String getBotUsername() {
        return BOT_USERNAME;
    }

    public static String getBotToken() {
        return BOT_TOKEN;
    }
    
    public static Long getBackupGroupId() {
        return BACKUP_GROUP_ID;
    }
    
    public static boolean isBackupEnabled() {
        return BACKUP_GROUP_ID != null;
    }
}