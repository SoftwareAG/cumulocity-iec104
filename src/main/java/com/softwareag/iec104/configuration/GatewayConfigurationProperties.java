package com.softwareag.iec104.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.softwareag.iec104.repository.SqliteRepository;

@Component
public class GatewayConfigurationProperties {

	final Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private SqliteRepository sqliteRepository;
	
    @Value("${gateway.identifier:iec104}")
    private String identifier;
    @Value("${gateway.bootstrapFixedDelay:10000}")
    private Integer bootstrapFixedDelay;
    @Value("${iec104server.host}")
    private String host;
    @Value("${iec104server.port:2404}")
    private int port;
    @Value("${gateway.commonAddress:0}")
    private int commonAddress;
    @Value("${c8y.host}")
	private String c8yHost;
    @Value("${credentialsFile:cumulocityCredentials}")
    private String credentialsfile;
    @Value("${c8y.bootstrap.tenant}")
    private String bootstrapTenant;
    @Value("${c8y.bootstrap.username}")
    private String bootstrapUsername;
    @Value("${c8y.bootstrap.password}")
    private String bootstrapPassword;
    @Value("${c8y.bootstrap.url}")
    private String bootstrapUrl;
    @Value("${gateway.name:IEC104 Gateway}")
    private String gatewayName;
    @Value("${buffer.enabled:false}")
    private boolean bufferEnabled;
    @Value("${buffer.db.path}")
    private String bufferDbPath;
    @Value("${buffer.interval}")
    private int bufferInterval;
    @Value("${log.enabled}")
    private boolean logEnabled;
    @Value("${c8y.mqtt.host}")
    private String mqttHost;
    @Value("${c8y.mqtt.port}")
    private int mqttPort;
    @Value("${c8y.api.mode}")
    private String apiMode;


    public String getIdentifier() {
		return identifier;
	}
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	public Integer getBootstrapFixedDelay() {
		return bootstrapFixedDelay;
	}
	public void setBootstrapFixedDelay(Integer bootstrapFixedDelay) {
		this.bootstrapFixedDelay = bootstrapFixedDelay;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public int getCommonAddress() {
		return commonAddress;
	}
	public void setCommonAddress(int commonAddress) {
		this.commonAddress = commonAddress;
	}
	public String getC8yHost() {
		return c8yHost;
	}
	public void setC8yHost(String c8yHost) {
		this.c8yHost = c8yHost;
	}
	public String getCredentialsfile() {
		return credentialsfile;
	}
	public void setCredentialsfile(String credentialsfile) {
		this.credentialsfile = credentialsfile;
	}
	public String getBootstrapTenant() {
		return bootstrapTenant;
	}
	public void setBootstrapTenant(String bootstrapTenant) {
		this.bootstrapTenant = bootstrapTenant;
	}
	public String getBootstrapUsername() {
		return bootstrapUsername;
	}
	public void setBootstrapUsername(String bootstrapUsername) {
		this.bootstrapUsername = bootstrapUsername;
	}
	public String getBootstrapPassword() {
		return bootstrapPassword;
	}
	public void setBootstrapPassword(String bootstrapPassword) {
		this.bootstrapPassword = bootstrapPassword;
	}
	public String getBootstrapUrl() {
		return bootstrapUrl;
	}
	public void setBootstrapUrl(String bootstrapUrl) {
		this.bootstrapUrl = bootstrapUrl;
	}
	public String getGatewayName() {
		return gatewayName;
	}
	public void setGatewayName(String gatewayName) {
		this.gatewayName = gatewayName;
	}
	public boolean isBufferEnabled() {
		return bufferEnabled;
	}
	public void setBufferEnabled(boolean gatewayBuffer) {
		this.bufferEnabled = gatewayBuffer;
	}
	public String getBufferDbPath() {
		return bufferDbPath;
	}
	public void setBufferDbPath(String bufferDbPath) {
		this.bufferDbPath = bufferDbPath;
	}
	public int getBufferInterval() {
		return bufferInterval;
	}
	public void setBufferInterval(int bufferInterval) {
		this.bufferInterval = bufferInterval;
	}
	public String getMqttHost() {
		return mqttHost;
	}
	public void setMqttHost(String mqttHost) {
		this.mqttHost = mqttHost;
	}
	public int getMqttPort() {
		return mqttPort;
	}
	public void setMqttPort(int mqttPort) {
		this.mqttPort = mqttPort;
	}
	public String getApiMode() {
		return apiMode;
	}
	public void setApiMode(String apiMode) {
		this.apiMode = apiMode;
	}
	public boolean isLogEnabled() {
		return logEnabled;
	}
	public void setLogEnabled(boolean logEnabled) {
		this.logEnabled = logEnabled;
	}

	public void writeConfig() {
		System.out.println(System.getProperty("user.home"));
		Properties prop = new Properties();
		
		prop.put("gateway.identifier", identifier);
		prop.put("gateway.bootstrapFixedDelay", new Integer(bootstrapFixedDelay).toString());
		prop.put("iec104server.host", host);
		prop.put("iec104server.port", new Integer(port).toString());
		prop.put("gateway.commonAddress", new Integer(commonAddress).toString());
		prop.put("c8y.host", c8yHost);
		prop.put("credentialsFile", credentialsfile);
		prop.put("c8y.bootstrap.tenant", bootstrapTenant);
		prop.put("c8y.bootstrap.username", bootstrapUsername);
		prop.put("c8y.bootstrap.password", bootstrapPassword);
		prop.put("c8y.bootstrap.url", bootstrapUrl);
		prop.put("gateway.name", gatewayName);
		prop.put("buffer.enabled", new Boolean(bufferEnabled).toString());
		prop.put("buffer.db.path", bufferDbPath);
		prop.put("buffer.interval", new Integer(bufferInterval).toString());
		prop.put("log.enabled", new Boolean(logEnabled).toString());
		
		try {
			File iec104Folder = new File(System.getProperty("user.home") + "/.iec104");
			if (!iec104Folder.exists()) {
				iec104Folder.mkdir();
			}
			prop.store(new FileOutputStream(System.getProperty("user.home") + "/.iec104/iec104-agent-gateway.properties"), "");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getConfig() {
	    String config = MessageFormat.format("iec104server.host={0}\n"
	    		+ "iec104server.port={1,number,#}\n"
	    		+ "buffer.enabled={2}\n"
	    		+ "buffer.interval={3,number,#}\n"
	    		+ "log.enabled={4}",
	    		host,
	    		port,
	    		bufferEnabled,
	    		bufferInterval,
	    		logEnabled);
        
        return config;
	}
	
	public boolean updateConfig(String config) {
		logger.info("Reading new configuration...");
		Properties prop = new Properties();
		boolean restart = false;
		boolean changed = false;
		try {
			prop.load(new StringReader(config));
			String newHost = prop.getProperty("iec104server.host", host);
			int newPort = new Integer(prop.getProperty("iec104server.port", new Integer(port).toString()));
			boolean newBufferEnabled = new Boolean(prop.getProperty("buffer.enabled", new Boolean(bufferEnabled).toString()));
			int newBufferInterval = new Integer(prop.getProperty("buffer.interval", new Integer(bufferInterval).toString()));
			boolean newLogEnabled = new Boolean(prop.getProperty("log.enabled", new Boolean(logEnabled).toString()));
			if (!newHost.equals(host) || newPort != port) {
				restart = true;
				changed = true;
			}
			if (bufferInterval != newBufferInterval) {
				sqliteRepository.setBufferInterval(newBufferInterval);
				changed = true;
			}
			if (bufferEnabled != newBufferEnabled || logEnabled != newLogEnabled) {
				changed = true;
			}
			host = newHost;
			port = newPort;
			bufferEnabled = newBufferEnabled;
			bufferInterval = newBufferInterval;
			logEnabled = newLogEnabled;
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (changed) {
			logger.info("Configuration has changed. File will be updated.");
			writeConfig();
		} else {
			logger.info("No change detected.");
		}
		return restart;
	}
}
