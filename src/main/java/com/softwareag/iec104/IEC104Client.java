package com.softwareag.iec104;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openmuc.j60870.ASdu;
import org.openmuc.j60870.CauseOfTransmission;
import org.openmuc.j60870.ClientConnectionBuilder;
import org.openmuc.j60870.Connection;
import org.openmuc.j60870.ConnectionEventListener;
import org.openmuc.j60870.ie.IeDoublePointWithQuality;
import org.openmuc.j60870.ie.IeNormalizedValue;
import org.openmuc.j60870.ie.IeQualifierOfInterrogation;
import org.openmuc.j60870.ie.IeScaledValue;
import org.openmuc.j60870.ie.IeShortFloat;
import org.openmuc.j60870.ie.IeSingleCommand;
import org.openmuc.j60870.ie.IeSinglePointWithQuality;
import org.openmuc.j60870.ie.IeTime56;
import org.openmuc.j60870.ie.InformationElement;
import org.openmuc.j60870.ie.InformationObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cumulocity.rest.representation.inventory.ManagedObjectRepresentation;
import com.softwareag.iec104.configuration.GatewayConfigurationProperties;
import com.softwareag.iec104.model.AsduMapping;
import com.softwareag.iec104.model.Device;
import com.softwareag.iec104.repository.CumulocityRepository;
import com.softwareag.iec104.service.BootstrapService;

@Service
public class IEC104Client {

	private static final String GATEWAY_EXTERNAL_ID_TYPE = "IEC104_originatorAddress";
	private static final String DEVICE_EXTERNAL_ID_TYPE = "IEC104_commonAddress";
	private static final String IEC104_GATEWAY = "IEC104_Gateway";
	private static final String IEC104_DEVICE = "IEC104_Device";

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private BootstrapService bootstrapService;

	@Autowired
	private GatewayConfigurationProperties properties;

	@Autowired
	private CumulocityRepository cumulocityRepository;

	private Connection connection;

	@Autowired
	private AsduMapping asduMapping;
	
	private Map<Integer, ManagedObjectRepresentation> devices = new HashMap<>();
	
	private ManagedObjectRepresentation gateway;

	public enum DataType {
		MEASUREMENT {
			@Override
			public void map(IEC104Client client, ASdu aSdu, ManagedObjectRepresentation mor, InformationObject io,
					Map<String, String> data) {
				client.cumulocityRepository.sendMeasurement(mor, data.get("fragment"), data.get("series"),
						data.get("unit"), client.getBigDecimalValue(aSdu, data, io), client.getDateTime(io, data));
			}

		},
		ENUMERATION {
			@Override
			public void map(IEC104Client client, ASdu aSdu, ManagedObjectRepresentation mor, InformationObject io,
					Map<String, String> data) {
				String name = data.get("name");
				String text = data.get(client.getStringValue(aSdu, data, io));
				if (text != null) {
					client.cumulocityRepository.sendEvent(mor, name, text, client.getDateTime(io, data));
				} else {
					client.logger.error("Unsupported value {}", client.getStringValue(aSdu, data, io));
				}
			}
		},
		EVENT {
			@Override
			public void map(IEC104Client client, ASdu aSdu, ManagedObjectRepresentation mor, InformationObject io,
					Map<String, String> data) {
			}
		},
		ALARM {
			@Override
			public void map(IEC104Client client, ASdu aSdu, ManagedObjectRepresentation mor, InformationObject io,
					Map<String, String> data) {
				if (client.getBooleanValue(aSdu, data, io)) {
					client.cumulocityRepository.sendAlarm(mor, data.get("type"), data.get("text"), data.get("severity"),
							client.getDateTime(io, data));
				} else {
					client.cumulocityRepository.clearAlarm(mor, data.get("type"), client.getDateTime(io, data));
				}

			}
		};

		public abstract void map(IEC104Client client, ASdu aSdu, ManagedObjectRepresentation mor, InformationObject io,
				Map<String, String> data);
	}

	private class ClientEventListener implements ConnectionEventListener {

		@Override
		public void newASdu(ASdu aSdu) {
			logger.info("\nReceived ASDU:\n{}", aSdu.toString());
			
			if (asduMapping.getDevices().containsKey(aSdu.getOriginatorAddress().toString())
					&& asduMapping.getDevices().get(aSdu.getOriginatorAddress().toString())
							.containsKey(new Integer(aSdu.getCommonAddress()).toString())) {
				Device device = asduMapping.getDevices().get(aSdu.getOriginatorAddress().toString())
						.get(new Integer(aSdu.getCommonAddress()).toString());
				ManagedObjectRepresentation mor;
				if (!devices.containsKey(aSdu.getCommonAddress())) {
					mor = cumulocityRepository.upsertDevice(
							aSdu.getOriginatorAddress() + "_" + aSdu.getCommonAddress(), device.getName(), IEC104_DEVICE,
							DEVICE_EXTERNAL_ID_TYPE, gateway);
					devices.put(aSdu.getCommonAddress(), mor);
				} else {
					mor = devices.get(aSdu.getCommonAddress());
				}
				logger.info("ASDU is mapped to device {}", mor.getId());
				if (properties.isLogEnabled()) {
					cumulocityRepository.logAsdu(mor, aSdu);
				}
				if (device.getData().containsKey(aSdu.getTypeIdentification())) {
					for (InformationObject io : aSdu.getInformationObjects()) {
						if (device.getData().get(aSdu.getTypeIdentification())
								.containsKey(new Integer(io.getInformationObjectAddress()).toString())) {
							Map<DataType, Map<String, String>> dataType = device.getData()
									.get(aSdu.getTypeIdentification())
									.get(new Integer(io.getInformationObjectAddress()).toString());
							for (DataType dt : dataType.keySet()) {
								dt.map(IEC104Client.this, aSdu, mor, io, dataType.get(dt));
							}
						}
					}
				}
			} else {
				logger.info("ASDU is not mapped");
				if (properties.isLogEnabled()) {
					cumulocityRepository.logAsdu(gateway, aSdu);
				}
			}
		}

		@Override
		public void connectionClosed(IOException e) {
			logger.info("Received connection closed signal. Reason: ");
			if (!e.getMessage().isEmpty()) {
				logger.info(e.getMessage());
			} else {
				logger.info("unknown");
			}
			logger.info("Reconnecting...");
			start();
		}

	}

	private DateTime getDateTime(InformationObject io, Map<String, String> data) {
		String[] xy;
		int x;
		int y;
		DateTime dateTime = DateTime.now();
		if (data.containsKey("time")) {
			xy = data.get("time").split(":");
			x = new Integer(xy[0]);
			y = new Integer(xy[1]);
			dateTime = getDateTime(io, x, y);
		}
		return dateTime;
	}

	private BigDecimal getBigDecimalValue(ASdu aSdu, Map<String, String> data, InformationObject io) {
		String[] xy = data.get("value").split(":");
		int x = new Integer(xy[0]);
		int y = new Integer(xy[1]);
		Class<InformationElement> valueType = asduMapping.getAsdus().get(aSdu.getTypeIdentification()).get(x).get(y);
		InformationElement ie = io.getInformationElements()[x][y];
		BigDecimal value = null;
		if (valueType.equals(IeNormalizedValue.class)) {
			value = BigDecimal.valueOf(((IeNormalizedValue) ie).getNormalizedValue());
		} else if (valueType.equals(IeScaledValue.class)) {
			value = BigDecimal.valueOf(((IeScaledValue) ie).getNormalizedValue());
		} else if (valueType.equals(IeShortFloat.class)) {
			value = BigDecimal.valueOf(((IeShortFloat) ie).getValue());
		} else {
			logger.error("Unsupported data type to convert to BigDecimal: " + valueType);
		}
		if (data.containsKey("offset")) {
			value = value.add(new BigDecimal(data.get("offset")));
		}
		if (data.containsKey("multiplier")) {
			value = value.multiply(new BigDecimal(data.get("multiplier")));
		}
		return value;
	}

	private String getStringValue(ASdu aSdu, Map<String, String> data, InformationObject io) {
		String[] xy = data.get("value").split(":");
		int x = new Integer(xy[0]);
		int y = new Integer(xy[1]);
		Class<InformationElement> valueType = asduMapping.getAsdus().get(aSdu.getTypeIdentification()).get(x).get(y);
		InformationElement ie = io.getInformationElements()[x][y];
		String value = null;
		if (valueType.equals(IeNormalizedValue.class)) {
			value = new Double(((IeNormalizedValue) ie).getNormalizedValue()).toString();
		} else if (valueType.equals(IeScaledValue.class)) {
			value = new Double(((IeScaledValue) ie).getNormalizedValue()).toString();
		} else if (valueType.equals(IeShortFloat.class)) {
			value = new Float(((IeShortFloat) ie).getValue()).toString();
		} else if (valueType.equals(IeDoublePointWithQuality.class)) {
			value = ((IeDoublePointWithQuality) ie).getDoublePointInformation().toString();
		} else if (valueType.equals(IeSinglePointWithQuality.class)) {
			value = ((IeSinglePointWithQuality) ie).isOn() ? "1" : "0";
		} else {
			logger.error("Unsupported data type to convert to String: {}", valueType);
		}
		logger.info("Retrieved text value: {}", value);
		return value;
	}

	private Boolean getBooleanValue(ASdu aSdu, Map<String, String> data, InformationObject io) {
		String[] xy = data.get("value").split(":");
		int x = new Integer(xy[0]);
		int y = new Integer(xy[1]);
		Class<InformationElement> valueType = asduMapping.getAsdus().get(aSdu.getTypeIdentification()).get(x).get(y);
		InformationElement ie = io.getInformationElements()[x][y];
		Boolean value = null;
		if (valueType.equals(IeSinglePointWithQuality.class)) {
			value = ((IeSinglePointWithQuality) ie).isOn();
		} else {
			logger.error("Unsupported data type to convert to Boolean: {}", valueType);
		}
		logger.info("Retrieved text value: {}", value);
		return value;
	}

	private DateTime getDateTime(InformationObject io, int x, int y) {
		IeTime56 time56 = (IeTime56) io.getInformationElements()[x][y];
		DateTime dateTime;
		if (time56 != null) {
			dateTime = new DateTime(time56.getTimestamp(), DateTimeZone.forTimeZone(time56.getTimeZone()));
		} else {
			dateTime = DateTime.now();
		}
		return dateTime;
	}

	public void start() {
		bootstrapService.bootstrap();
		cumulocityRepository.setPlatform(bootstrapService.getPlatform());
		cumulocityRepository.setMqttClient(bootstrapService.getMqttClient());
		if (gateway == null) {
			gateway = cumulocityRepository.upsertDevice(properties.getIdentifier(),
					properties.getGatewayName(), IEC104_GATEWAY, GATEWAY_EXTERNAL_ID_TYPE, null);
		}

		InetAddress address;
		try {
			address = InetAddress.getByName(properties.getHost());
		} catch (UnknownHostException e) {
			logger.info("Unknown host: {}", properties.getHost());
			return;
		}

		ClientConnectionBuilder clientConnectionBuilder = new ClientConnectionBuilder(address)
				.setMessageFragmentTimeout(5000).setPort(properties.getPort());

		boolean connected = false;

		while (!connected) {
			try {
				connection = clientConnectionBuilder.build();
				connected = true;
			} catch (IOException e) {
				logger.info("Unable to connect to remote host: {}. Retrying...", properties.getHost());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}

		boolean dataTransferStarted = false;

		while (!dataTransferStarted) {
			try {
				logger.info("Send start DT.");
				connection.startDataTransfer(new ClientEventListener(), 5000);
				dataTransferStarted = true;
			} catch (InterruptedIOException e2) {
				logger.info("Got Timeout. Retrying...");
			} catch (IOException e) {
				logger.info("Connection closed for the following reason: {}. Retrying...", e.getMessage());
			}
		}
		logger.info("successfully connected");
		try {
			connection.interrogation(properties.getCommonAddress(), CauseOfTransmission.ACTIVATION,
					new IeQualifierOfInterrogation(20));
			connection.synchronizeClocks(properties.getCommonAddress(), new IeTime56(System.currentTimeMillis()));
			connection.singleCommand(properties.getCommonAddress(), CauseOfTransmission.ACTIVATION, 5000,
					new IeSingleCommand(true, 0, true));
			connection.singleCommand(properties.getCommonAddress(), CauseOfTransmission.ACTIVATION, 5000,
					new IeSingleCommand(false, 0, false));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void restart() {
		logger.info("Restarting agent...");
		if (connection != null) {
			connection.close();
		}
		start();
	}
}
