package com.simonscholz.bot.weather.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class TelegramConfig {

    @Bean
    @Qualifier("telegram")
    public WebClient webclientTelegram(
            @Value("${telegram.bot.url:https://api.telegram.org/bot}") String telegramBotUrl,
            Environment environment) {
        String telegramBotToken = environment.getProperty("telegram.bot.token");
        return WebClient.create(telegramBotUrl + telegramBotToken);
    }

    @Bean
    @Qualifier("dmi")
    public WebClient webclientDmi(@Value("${dmi.url:http://www.dmi.dk/Data4DmiDk/}") String dmiUrl) {
        return WebClient.create(dmiUrl);
    }
}
