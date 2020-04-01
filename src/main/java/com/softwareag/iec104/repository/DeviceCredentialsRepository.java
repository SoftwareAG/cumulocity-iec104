package com.softwareag.iec104.repository;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.stereotype.Repository;
import org.springframework.util.DefaultPropertiesPersister;

@Repository
public class DeviceCredentialsRepository {
	private Properties properties = new Properties();
	public String getTenant() {
		return properties.getProperty("tenant");
	}
	public String getUsername() {
		return properties.getProperty("username");
	}
	public String getPassword() {
		return properties.getProperty("password");
	}
	public void setTenant(String tenant) {
		properties.setProperty("tenant", tenant);
	}
	public void setUsername(String username) {
		properties.setProperty("username", username);
	}
	public void setPassword(String password) {
		properties.setProperty("password", password);
	}

	public void loadCredentials(String url) {
		DefaultPropertiesPersister propertiesPersister = new DefaultPropertiesPersister();
		try {
			propertiesPersister.load(properties, new FileInputStream(url));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void saveCredentials(String url) {
		DefaultPropertiesPersister propertiesPersister = new DefaultPropertiesPersister();
		try {
			propertiesPersister.store(properties, new FileOutputStream(url), "Cumulocity credentials");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
