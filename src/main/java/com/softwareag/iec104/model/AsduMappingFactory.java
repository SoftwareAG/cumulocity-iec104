package com.softwareag.iec104.model;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AsduMappingFactory implements PropertySourceFactory {

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Override
	public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return new MapPropertySource("asdu", mapper.readValue(resource.getInputStream(), Map.class));
	}

}
