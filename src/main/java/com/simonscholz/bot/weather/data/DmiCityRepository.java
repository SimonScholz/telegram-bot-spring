package com.simonscholz.bot.weather.data;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.simonscholz.bot.weather.domain.DmiCity;

import reactor.core.publisher.Mono;

public interface DmiCityRepository extends ReactiveCrudRepository<DmiCity, Integer> {

    Mono<DmiCity> findByLabelContainingIgnoreCase(String city);
}
