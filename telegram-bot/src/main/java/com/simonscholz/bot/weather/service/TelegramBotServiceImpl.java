package com.simonscholz.bot.weather.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.simonscholz.bot.weather.domain.DmiCity;

import de.simonscholz.telegram.bot.api.TelegramHelper;
import de.simonscholz.telegram.bot.api.domain.Chat;
import de.simonscholz.telegram.bot.api.domain.Location;
import de.simonscholz.telegram.bot.api.domain.Message;
import de.simonscholz.telegram.bot.api.domain.Update;
import reactor.core.publisher.Mono;

@Component
public class TelegramBotServiceImpl implements TelegramBotService {

	private static final Logger LOG = LoggerFactory.getLogger(TelegramBotServiceImpl.class);
	private WebClient telegramWebClient;
	private WebClient dmiWebClient;

	public TelegramBotServiceImpl(@Qualifier("telegram") WebClient telegramWebClient,
			@Qualifier("dmi") WebClient dmiWebClient) {
		this.telegramWebClient = telegramWebClient;
		this.dmiWebClient = dmiWebClient;
	}

	@Override
	public Mono<Void> webhook(Mono<Update> update) {
		
		Mono<MultiValueMap<String, String>> dmiCityFlux = dmiWebClient.get()
				.uri(qBuilder -> qBuilder.path("/getData").queryParam("type", "forecast")
						.queryParam("term", "Hamburg").build())
				.retrieve().bodyToFlux(DmiCity.class).next().map(dmiCity -> {
					MultiValueMap<String, String> formData = new LinkedMultiValueMap<>(2);

					formData.set("chat_id", "****");
					formData.set("photo", "http://servlet.dmi.dk/byvejr/servlet/world_image?city=2911298&mode=dag1_2");
					
					return formData;
				});

		ParameterizedTypeReference<MultiValueMap<String, String>> typeReference = new ParameterizedTypeReference<MultiValueMap<String, String>>() {
		};

		return telegramWebClient.post().uri("/sendPhoto")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(BodyInserters.fromPublisher(dmiCityFlux, typeReference))
				.exchange().then();
	}

//	@Override
//	public Mono<Void> webhook(Mono<Update> update) {
//		return update.map(u -> {
//
//			Optional<Message> messageOptional = TelegramHelper.getMessage(u);
//
//			if (messageOptional.isPresent()) {
//				Message message = messageOptional.get();
//
//				long chatId = message.getChat().getId();
//				String text = message.getText();
//				Location location = message.getLocation();
//
//				if (text != null) {
//					LOG.debug("Chat id:" + chatId);
//					LOG.debug("Text : " + text);
//
//					int indexOf = text.indexOf(" ");
//
//					if (indexOf > -1) {
//						String queryString = text.substring(indexOf).trim();
//
//						if (text.startsWith("/now")) {
//
//							Mono<MultiValueMap<String, String>> dmiCityFlux = dmiWebClient.get()
//									.uri(qBuilder -> qBuilder.path("/getData").queryParam("type", "forecast")
//											.queryParam("term", queryString).build())
//									.retrieve().bodyToFlux(DmiCity.class).next().map(dmiCity -> {
//										MultiValueMap<String, String> formData = new LinkedMultiValueMap<>(2);
//
//										formData.set("chat_id", String.valueOf(message.getChat().getId()));
//										formData.set("photo", "http://servlet.dmi.dk/byvejr/servlet/world_image?city=2911298&mode=dag1_2");
//										
//										return formData;
//									});
//
//							ParameterizedTypeReference<MultiValueMap<String, String>> typeReference = new ParameterizedTypeReference<MultiValueMap<String, String>>() {
//							};
//
//							return telegramWebClient.post().uri("/sendPhoto")
//									.contentType(MediaType.APPLICATION_FORM_URLENCODED).body(dmiCityFlux, typeReference)
//									.exchange().then();
//
//							Mono<Integer> dmiCityId = dmiCityService.getDmiCityId(queryString.trim());
//							sendDmiPhoto(chatId, dmiCityId, DmiApi.MODE_NOW);
//						} else if (text.startsWith("/week")) {
//							Mono<Integer> dmiCityId = dmiCityService.getDmiCityId(queryString.trim());
//							sendDmiPhoto(chatId, dmiCityId, DmiApi.MODE_WEEK);
//						}
//					} else if (text.toLowerCase().startsWith("/chatid")) {
//						long id = message.getChat().getId();
//						botClient.sendMessage(id, "Your chat id is: " + id).subscribe();
//					} else if (text.startsWith("/start") || text.startsWith("/help")) {
//						String username = TelegramHelper.getUserName(message);
//						StringBuilder sb = new StringBuilder();
//						sb.append("Hello ");
//						sb.append(username);
//						sb.append(",");
//						sb.append(System.lineSeparator());
//						sb.append(System.lineSeparator());
//						sb.append("Nice to meet you. I am the Dmi.dk weather bot.");
//						sb.append(System.lineSeparator());
//						sb.append("I was developed by Simon Scholz, a java developer, located in Hamburg.");
//						sb.append(System.lineSeparator());
//						sb.append("My source code can be found here: https://github.com/SimonScholz/telegram-bot/");
//						sb.append(System.lineSeparator());
//						sb.append(System.lineSeparator());
//						sb.append("But enough of this technical stuff.");
//						sb.append(System.lineSeparator());
//						sb.append("You wanna have these nice dmi.dk weather charts, right? ");
//						sb.append(System.lineSeparator());
//						sb.append(
//								"You can get these by using the /now + {your home town name} or /week + {your home town name} or by simply sending me your location. ");
//						sb.append(System.lineSeparator());
//						sb.append(
//								"The /now command shows the weather forecast for the next 3 days and the /week command is used for the week beginning after the next 3 days.");
//						botClient.sendMessage(chatId, sb.toString()).subscribe(m -> {
//							LOG.debug(m.getText());
//						});
//					} else {
//						Chat chat = message.getChat();
//						if (Chat.TYPE_PRIVATE.equals(chat.getType())) {
//							Mono<Message> sendMessage = botClient.sendMessage(chatId,
//									"This is not a proper command. \n You can send /help to get help.");
//							sendMessage.subscribe(m -> {
//								LOG.debug(m.getText());
//							});
//						}
//					}
//				} else if (location != null) {
//					Mono<Integer> dmiCityId = dmiCityService.getDmiCityId(location.getLongitude(),
//							location.getLatitude());
//					sendDmiPhoto(chatId, dmiCityId, DmiApi.MODE_NOW);
//				}
//			}
//			return Mono.empty();
//		}).then();
//	}
}
