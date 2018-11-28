package com.simonscholz.bot.weather.service;

import de.simonscholz.telegram.bot.api.domain.Update;
import reactor.core.publisher.Mono;

public interface TelegramBotService {
    Mono<Void> webhook(Mono<Update> update);
}
