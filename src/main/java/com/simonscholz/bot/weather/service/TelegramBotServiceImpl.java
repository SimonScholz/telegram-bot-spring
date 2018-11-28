package com.simonscholz.bot.weather.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

import com.simonscholz.bot.weather.data.DmiCityRepository;
import com.simonscholz.bot.weather.domain.DmiCity;

import de.simonscholz.telegram.bot.api.TelegramHelper;
import de.simonscholz.telegram.bot.api.domain.Update;
import reactor.core.publisher.Mono;

@Component
public class TelegramBotServiceImpl implements TelegramBotService {

    private static final String NOW_MODE = "dag1_2";
    private static final String WEEK_MODE = "dag3_9";

    private static final ParameterizedTypeReference<MultiValueMap<String, String>> TYPE_REFERENCE =
            new ParameterizedTypeReference<MultiValueMap<String, String>>() { };

    private static final Logger LOG = LoggerFactory.getLogger(TelegramBotServiceImpl.class);
    private WebClient telegramWebClient;
    private WebClient dmiWebClient;
    private DmiCityRepository dmiCityRepository;

    public TelegramBotServiceImpl(@Qualifier("telegram") WebClient telegramWebClient,
            @Qualifier("dmi") WebClient dmiWebClient, DmiCityRepository dmiCityRepository) {
        this.telegramWebClient = telegramWebClient;
        this.dmiWebClient = dmiWebClient;
        this.dmiCityRepository = dmiCityRepository;
    }

    @Override
    public Mono<Void> webhook(Mono<Update> update) {
        return update.map(u -> TelegramHelper.getMessage(u).get()).flatMap(message -> {
            String chatId = String.valueOf(message.getChat().getId());
            String text = message.getText();
            LOG.debug("Chat id:" + chatId);
            LOG.debug("Text : " + text);

            if (text.startsWith("/start") || text.startsWith("/help")) {
                String username = TelegramHelper.getUserName(message);
                String startText = buildStartText(username);

                return sendMessage(chatId, startText);
            }

            int indexOf = text.indexOf(" ");

            if (indexOf > -1) {
                String queryCityString = text.substring(indexOf).trim();
                try {
                    queryCityString = URLEncoder.encode(queryCityString, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    LOG.error(e.getMessage(), e);
                }

                String encodedCityString = queryCityString;

                if (text.startsWith("/now") || text.startsWith("/week")) {
                    Mono<MultiValueMap<String, String>> formDataMono = dmiCityRepository
                            .findByLabelContainingIgnoreCase(queryCityString)
                            .switchIfEmpty(dmiWebClient.get()
                                    .uri(qBuilder -> qBuilder.path("/getData").queryParam("type", "forecast")
                                            .queryParam("term", encodedCityString).build())
                                    .retrieve().bodyToFlux(DmiCity.class).next())
                            .flatMap(dmiCityRepository::save).map(dmiCity -> {
                                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>(2);
                                formData.set("chat_id", chatId);

                                String mode = NOW_MODE;
                                if (text.startsWith("/week")) {
                                    mode = WEEK_MODE;
                                }

                                // added System.currentTimeMillis() at the end of the image url, because
                                // telegram caches image urls
                                String dmiUrl = String.format(
                                        "http://servlet.dmi.dk/byvejr/servlet/world_image?city=%s&mode=%s&time=%s",
                                        dmiCity.getId(), mode, System.currentTimeMillis());
                                formData.set("photo", dmiUrl);

                                return formData;
                            });

                    return telegramWebClient.post().uri("/sendPhoto")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(BodyInserters.fromPublisher(formDataMono, TYPE_REFERENCE)).exchange().then();
                }
            }
            return Mono.empty();
        });
    }

    private Mono<? extends Void> sendMessage(String chatId, String text) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>(2);
        formData.set("chat_id", chatId);
        formData.set("text", text);
        return telegramWebClient.post().uri("/sendMessage").contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData)).exchange().then();
    }

    private String buildStartText(String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello ");
        sb.append(username);
        sb.append(",");
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("Nice to meet you. I am the Dmi.dk weather bot.");
        sb.append(System.lineSeparator());
        sb.append("I was developed by Simon Scholz, a java developer, located in Hamburg.");
        sb.append(System.lineSeparator());
        sb.append("My source code can be found here: https://github.com/SimonScholz/telegram-bot/");
        sb.append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("But enough of this technical stuff.");
        sb.append(System.lineSeparator());
        sb.append("You wanna have these nice dmi.dk weather charts, right? ");
        sb.append(System.lineSeparator());
        sb.append("You can get these by using the /now + {your home town name}");
        sb.append(" or /week + {your home town name} or by simply sending me your location.");
        sb.append(System.lineSeparator());
        sb.append("The /now command shows the weather forecast for the next 3 days ");
        sb.append("and the /week command is used for the week beginning after the next 3 days.");
        return sb.toString();
    }
}
