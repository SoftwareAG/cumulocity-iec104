package com.softwareag.iec104;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

@SpringBootApplication
@EnableConfigurationProperties
@PropertySources(value = {
		@PropertySource(value = "classpath:META-INF/spring/iec104-agent-gateway.properties", ignoreResourceNotFound = true),
		@PropertySource(value = "file:${iec104.conf.dir:/etc}/iec104/iec104-agent-gateway.properties", ignoreResourceNotFound = true),
        @PropertySource(value = "file:${user.home}/.iec104/iec104-agent-gateway.properties", ignoreResourceNotFound = true)
})
public class App {

	final static Logger logger = LoggerFactory.getLogger(App.class);

	public static void main(String[] args) {
		SpringApplication.run(App.class, args).getBean(IEC104Client.class).start();
	}
}