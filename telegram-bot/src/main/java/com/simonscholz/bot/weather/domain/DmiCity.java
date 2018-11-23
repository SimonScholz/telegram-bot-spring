package com.simonscholz.bot.weather.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DmiCity {
	private int id;

	private String label;

	private double longitude;

	private double latitude;

	private String country;

	private String country_code;

	private String city;
}
