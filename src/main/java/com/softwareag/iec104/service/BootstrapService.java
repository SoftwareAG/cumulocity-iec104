package com.softwareag.iec104.service;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.internal.MqttPersistentData;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cumulocity.model.authentication.CumulocityBasicCredentials;
import com.cumulocity.rest.representation.devicebootstrap.DeviceCredentialsRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.PlatformBuilder;
import com.cumulocity.sdk.client.polling.PollingStrategy;
import com.softwareag.iec104.configuration.GatewayConfigurationProperties;
import com.softwareag.iec104.repository.DeviceCredentialsRepository;

@Service
public class BootstrapService {

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GatewayConfigurationProperties properties;
	
	@Autowired
	private DeviceCredentialsRepository deviceCredentialsRepository;
	
	private Platform platform;
	
	private IMqttClient mqttClient;
	
	public void bootstrap() {
		if (new File(properties.getCredentialsfile()).exists()) {
			deviceCredentialsRepository.loadCredentials(properties.getCredentialsfile());
			CumulocityBasicCredentials userCredentials = CumulocityBasicCredentials.builder().tenantId(deviceCredentialsRepository.getTenant()).password(deviceCredentialsRepository.getPassword()).username(deviceCredentialsRepository.getUsername()).build();
			platform = PlatformBuilder.platform().withBaseUrl(properties.getC8yHost()).withCredentials(userCredentials).build();
			try {
				mqttClient = new MqttClient("tcp://" + properties.getMqttHost() + ":" + properties.getMqttPort(), properties.getIdentifier(), new MemoryPersistence());
				String mqttUsername = deviceCredentialsRepository.getTenant() + "/" + deviceCredentialsRepository.getUsername();
				MqttConnectOptions options = new MqttConnectOptions();
				options.setUserName(mqttUsername);
				options.setPassword(deviceCredentialsRepository.getPassword().toCharArray());
				mqttClient.connect(options);
				mqttClient.subscribe("s/e", new IMqttMessageListener() {
					
					@Override
					public void messageArrived(String arg0, MqttMessage arg1) throws Exception {
						logger.error(new String(arg1.getPayload()));
					}
				});
			} catch (MqttException e) {
				e.printStackTrace();
			}
		} else {
			logger.info("Will try to register gateway with id {} using following credentials:\nTenant: {}\nUsername: {}\nPassword: {}\nURL: {}", properties.getIdentifier(), properties.getBootstrapTenant(), properties.getBootstrapUsername(), properties.getBootstrapPassword(), properties.getBootstrapUrl());
			CumulocityBasicCredentials userCredentials = CumulocityBasicCredentials.builder().tenantId(properties.getBootstrapTenant()).password(properties.getBootstrapPassword()).username(properties.getBootstrapUsername()).build();
			platform = PlatformBuilder.platform().withBaseUrl(properties.getBootstrapUrl()).withCredentials(userCredentials).build();
			DeviceCredentialsRepresentation dcr = getCredentials();
			System.out.println(dcr);
			deviceCredentialsRepository.setTenant(dcr.getTenantId());
			deviceCredentialsRepository.setUsername(dcr.getUsername());
			deviceCredentialsRepository.setPassword(dcr.getPassword());
			deviceCredentialsRepository.saveCredentials(properties.getCredentialsfile());
			bootstrap();
		}
	}
	
	private DeviceCredentialsRepresentation getCredentials() {
		DeviceCredentialsRepresentation dcr;
		PollingStrategy strategy = new PollingStrategy(100L, TimeUnit.SECONDS, 2L);
		dcr = platform.getDeviceCredentialsApi().pollCredentials(properties.getIdentifier(), strategy);
		
		return dcr;
	}
	
	public Platform getPlatform() {
		return platform;
	}
	
	public IMqttClient getMqttClient() {
		return mqttClient;
	}
}
