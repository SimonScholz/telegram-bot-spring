package com.simonscholz.bot.weather.config;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RequestPredicates.contentType;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import com.simonscholz.bot.weather.data.DmiCityRepository;
import com.simonscholz.bot.weather.domain.DmiCity;
import com.simonscholz.bot.weather.service.TelegramBotService;

import de.simonscholz.telegram.bot.api.domain.Update;

@Configuration
public class TelegramRouter {
	@Bean
	public RouterFunction<ServerResponse> route(TelegramBotService botService, DmiCityRepository dmiCityRepository) {
		return RouterFunctions
				.route(POST("/telegram/webhook").and(accept(MediaType.APPLICATION_JSON))
						.and(contentType(MediaType.APPLICATION_JSON)),
						request -> ok().build(botService.webhook(request.bodyToMono(Update.class))))
				.andRoute(GET("/query"), req -> ok().body(dmiCityRepository.findAll(), DmiCity.class))
				.andRoute(GET("/query/{city}"),
						req -> ok().body(dmiCityRepository.findByLabelContainingIgnoreCase(req.pathVariable("city")),
								DmiCity.class));
	}
}
