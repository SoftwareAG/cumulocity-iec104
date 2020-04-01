package com.softwareag.iec104.repository;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.openmuc.j60870.ASdu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Repository;

import com.cumulocity.model.Agent;
import com.cumulocity.model.ID;
import com.cumulocity.model.idtype.GId;
import com.cumulocity.model.measurement.MeasurementValue;
import com.cumulocity.rest.representation.alarm.AlarmRepresentation;
import com.cumulocity.rest.representation.event.EventRepresentation;
import com.cumulocity.rest.representation.identity.ExternalIDRepresentation;
import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.cumulocity.rest.representation.measurement.MeasurementRepresentation;
import com.cumulocity.rest.representation.operation.OperationRepresentation;
import com.cumulocity.sdk.client.Platform;
import com.cumulocity.sdk.client.SDKException;
import com.cumulocity.sdk.client.alarm.AlarmApi;
import com.cumulocity.sdk.client.alarm.AlarmFilter;
import com.cumulocity.sdk.client.devicecontrol.DeviceControlApi;
import com.cumulocity.sdk.client.event.EventApi;
import com.cumulocity.sdk.client.identity.IdentityApi;
import com.cumulocity.sdk.client.inventory.InventoryApi;
import com.cumulocity.sdk.client.measurement.MeasurementApi;
import com.cumulocity.sdk.client.notification.Subscriber;
import com.softwareag.iec104.IEC104Client;
import com.softwareag.iec104.configuration.GatewayConfigurationProperties;

import c8y.Configuration;
import c8y.IsDevice;
import c8y.RequiredAvailability;
import c8y.SupportedOperations;

@Repository
@EnableScheduling
public class CumulocityRepository {

	final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private GatewayConfigurationProperties properties;
	
	@Autowired
	private SqliteRepository sqliteRepository;
	
	@Autowired
	private IEC104Client iec104Client;

	private Platform platform;
	
	private IMqttClient mqttClient;
	
	private InventoryApi inventoryApi;
	
	private IdentityApi identityApi;
    
    private EventApi eventApi;
    
    private MeasurementApi measurementApi;
    
    private AlarmApi alarmApi;
    
    private DeviceControlApi deviceControlApi;
    
    private Subscriber<GId, OperationRepresentation> operationSubscriber;
    
    private GId gatewayId;
    
    private Map<GId, String> externalIds = new HashMap<>();

	public Platform getPlatform() {
		return platform;
	}
	
	@PostConstruct
	private void initRepo() {
		if (properties.isBufferEnabled()) {
			sqliteRepository.createMeasurementTable();
			sqliteRepository.createMqttTable();
			sqliteRepository.createBufferConfigTable();
			if (sqliteRepository.getBufferInterval() == null) {
				sqliteRepository.setBufferInterval(properties.getBufferInterval());
			} else {
				properties.setBufferInterval(sqliteRepository.getBufferInterval());
			}
		}
	}

	public void setPlatform(Platform platform) {
		this.platform = platform;
		try {
			inventoryApi = platform.getInventoryApi();
			identityApi = platform.getIdentityApi();
			eventApi = platform.getEventApi();
			measurementApi = platform.getMeasurementApi();
			alarmApi = platform.getAlarmApi();
			deviceControlApi = platform.getDeviceControlApi();
		} catch (Exception e) {
			e.printStackTrace();
			logger.info("Unable to connect to Cumulocity tenant. Will restart in 5s...");
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			iec104Client.restart();
		}
	}
	
	public void setMqttClient(IMqttClient mqttClient) {
		this.mqttClient = mqttClient;
	}

	private ExternalIDRepresentation findExternalId(String externalId, String type) {
		ID id = new ID();
		id.setType(type);
		id.setValue(externalId);
		ExternalIDRepresentation extId = null;
		try {
			extId = identityApi.getExternalId(id);
		} catch (SDKException e) {
			logger.info("External ID {} not found", externalId);
		}
		return extId;
	}

	public ManagedObjectRepresentation upsertDevice(String id, String name, String type, String externalIdType,
			ManagedObjectRepresentation parent) {
		ManagedObjectRepresentation mor = null;
		try {
			logger.info("Upsert device with address {}", id);
			ExternalIDRepresentation extId = findExternalId(id, externalIdType);
			if (extId == null) {
				mor = new ManagedObjectRepresentation();
				mor.setType(type);
				mor.setName(name);
				mor.setLastUpdatedDateTime(null);
				if (parent == null) {
					mor.set(new IsDevice());
					mor.set(new Agent());
					mor = inventoryApi.create(mor);
					//operationSubscriber = deviceControlApi.getNotificationsSubscriber();
					//operationSubscriber.subscribe(mor.getId(), new OperationDispatcherSubscriptionListener());
				} else {
					mor = inventoryApi.getManagedObjectApi(parent.getId()).addChildDevice(mor);
				}
				extId = new ExternalIDRepresentation();
				extId.setExternalId(id);
				extId.setType(externalIdType);
				extId.setManagedObject(mor);
				identityApi.create(extId);
				extId = new ExternalIDRepresentation();
				extId.setExternalId(id);
				extId.setType("c8y_Serial");
				extId.setManagedObject(mor);
				identityApi.create(extId);
			} else {
				mor = extId.getManagedObject();
			}
			if (parent == null) {
				gatewayId = mor.getId();
				mor.setLastUpdatedDateTime(null);
				//Configuration config = new Configuration(properties.getConfig());
				SupportedOperations supportedOperations = new SupportedOperations();
				supportedOperations.add("c8y_Command");
				supportedOperations.add("c8y_Configuration");
				supportedOperations.add("c8y_SendConfiguration");
				supportedOperations.add("c8y_Software");
				supportedOperations.add("c8y_Firmware");
				supportedOperations.add("c8y_RemoteAccessConnect");
				mor.set(supportedOperations);
				//mor.set(config);
				if (properties.isBufferEnabled()) {
					RequiredAvailability requiredAvailability = new RequiredAvailability(properties.getBufferInterval() / 60);
					mor.set(requiredAvailability);
				}
				inventoryApi.update(mor);
				/*if (operationSubscriber == null) {
					operationSubscriber = deviceControlApi.getNotificationsSubscriber();
					operationSubscriber.subscribe(mor.getId(), new OperationDispatcherSubscriptionListener());
				}*/
			}
			externalIds.put(mor.getId(), extId.getExternalId());
		} catch (SDKException e) {
			logger.info("Error on upserting Device", e);
		}
		return mor;
	}
	
	public void updateGatewayConfig(Configuration config) {
		ManagedObjectRepresentation gateway = inventoryApi.get(gatewayId);
		gateway.set(config);
		gateway.setLastUpdatedDateTime(null);
		inventoryApi.update(gateway);
	}
	
	public void sendMeasurement(ManagedObjectRepresentation mor, String fragment, String series, String unit, BigDecimal value, DateTime time) {
		if (properties.getApiMode().equals("http")) {
			MeasurementRepresentation m = new MeasurementRepresentation();
			MeasurementValue mv = new MeasurementValue();
			Map<String, MeasurementValue> measurementValueMap = new HashMap<>();
			mv.setValue(value);
			mv.setUnit(unit);
			measurementValueMap.put(series, mv);
			m.set(measurementValueMap, fragment);
			m.setType(fragment);
			m.setSource(mor);
			m.setDateTime(time);
			logger.info("Will generate a measurement: {}", m.toJSON());
	
			if (properties.isBufferEnabled()) {
				sqliteRepository.insertMeasurement(m.toJSON());
			} else {
				measurementApi.create(m);
			}
		} else {
			String message = "200," + fragment + "," + series + "," + value.toPlainString() + "," + unit + "," + ISODateTimeFormat.dateTime().print(time);
			logger.info("{} <- {}", "s/us/" + externalIds.get(mor.getId()), message);
			if (properties.isBufferEnabled()) {
				sqliteRepository.insertMqtt("s/us/" + externalIds.get(mor.getId()), message);
			} else {
				try {
					mqttClient.publish("s/us/" + externalIds.get(mor.getId()), new MqttMessage(message.getBytes()));
				} catch (MqttPersistenceException e) {
					e.printStackTrace();
				} catch (MqttException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void sendAlarm(ManagedObjectRepresentation mor, String type, String text, String severity, DateTime time) {
		AlarmRepresentation a = new AlarmRepresentation();
		a.setType(type);
		a.setText(text);
		a.setSeverity(severity);
		a.setDateTime(time);
		a.setSource(mor);
		logger.info("Will generate an alarm: {}", a.toJSON());
		
		alarmApi.create(a);
	}
	
	public void clearAlarm(ManagedObjectRepresentation mor, String type, DateTime time) {
		alarmApi.getAlarmsByFilter(new AlarmFilter().byType(type).bySource(mor.getId())).get().forEach(a -> {
			a.setStatus("CLEARED");
			a.setDateTime(time);
			alarmApi.update(a);
		});
	}
	
	public void sendEvent(ManagedObjectRepresentation mor, String type, String text, DateTime time) {
		if (properties.getApiMode().equals("http")) {
	        EventRepresentation eventRepresentation = new EventRepresentation();
	        eventRepresentation.setSource(mor);
	        eventRepresentation.setDateTime(time);
	        eventRepresentation.setText(text);
	        eventRepresentation.setType(type);
			logger.info("Will generate an event: {}", eventRepresentation.toJSON());
	
			eventApi.create(eventRepresentation);
		} else {
			String message = "400," + type + ",\"" + text + "\"," + ISODateTimeFormat.dateTime().print(time);
			try {
				mqttClient.publish("s/us/" + externalIds.get(mor.getId()), new MqttMessage(message.getBytes()));
			} catch (MqttPersistenceException e) {
				e.printStackTrace();
			} catch (MqttException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void logAsdu(ManagedObjectRepresentation mor, ASdu aSdu) {
        EventRepresentation eventRepresentation = new EventRepresentation();
        eventRepresentation.setSource(mor);
        eventRepresentation.setDateTime(DateTime.now());
        eventRepresentation.setText(aSdu.toString());
        eventRepresentation.setType("ASDU");
        eventApi.create(eventRepresentation);
	}

    /*private class OperationDispatcherSubscriptionListener implements SubscriptionListener<GId, OperationRepresentation> {

        @Override
        public void onError(Subscription<GId> sub, Throwable e) {
            logger.error("OperationDispatcher error!", e);
        }

        @Override
        public void onNotification(Subscription<GId> sub, OperationRepresentation operation) {
            try {
                processOperation(operation);
            } catch (SDKException e) {
                logger.error("OperationDispatcher error!", e);
            }
        }
    }
    
    private void processOperation(OperationRepresentation o) {
    	logger.info("Processing operation: {}", o.toJSON());
    	
    	o.setStatus(OperationStatus.EXECUTING.name());
    	deviceControlApi.update(o);
    	if (o.hasProperty("c8y_Configuration")) {
    		Configuration config = o.get(Configuration.class);
    		updateGatewayConfig(config);
    		if (properties.updateConfig(config.getConfig())) {
    			iec104Client.restart();
    		}
    		o.setStatus(OperationStatus.SUCCESSFUL.name());
    		deviceControlApi.update(o);
    	} else {
    		o.setStatus(OperationStatus.FAILED.name());
    		o.setFailureReason("Unsupported operation: " + o.toJSON());
    		deviceControlApi.update(o);
    	}
    }
    
    @Scheduled(fixedDelay = 10000)
    private void processPendingOperations() {
    	if (deviceControlApi == null) return;
    	logger.info("Processing all pending operations for device {}...", gatewayId);
    	OperationFilter filter = new OperationFilter();
    	filter.byStatus(OperationStatus.PENDING);
    	filter.byDevice(gatewayId.getValue());
    	for(OperationRepresentation o : deviceControlApi.getOperationsByFilter(filter).get()) {
    		processOperation(o);
    	}
    }*/
}
