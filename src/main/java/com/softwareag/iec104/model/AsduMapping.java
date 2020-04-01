package com.softwareag.iec104.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openmuc.j60870.ASduType;
import org.openmuc.j60870.ie.InformationElement;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.softwareag.iec104.IEC104Client.DataType;

@Configuration
@PropertySources(value = {
		@PropertySource(value = "classpath:asdu.json", factory = AsduMappingFactory.class, ignoreResourceNotFound = true),
		@PropertySource(value = "file:${user.home}/.iec104/asdu.json", factory = AsduMappingFactory.class, ignoreResourceNotFound = true)
})
@ConfigurationProperties
public class AsduMapping {

	private Map<ASduType, List<List<Class<InformationElement>>>> asdus = new HashMap<>();
	@JsonIgnore
	private Map<String, Map<String, Device>> devices = new HashMap<>();

	public Map<ASduType, List<List<Class<InformationElement>>>> getAsdus() {
		return asdus;
	}

	public Map<String, Map<String, Device>> getDevices() {
		return devices;
	}

	public void setAsdus(Map<ASduType, List<List<Class<InformationElement>>>> asdus) {
		this.asdus = asdus;
	}

	//TODO don't have time to better handle unmarshalling -> Jackson can't correctly map the json config file so doing it by hand
	@JsonProperty("devices")
	public void setDevices(Map<String, Map<String, Map<?, ?>>> devices) {
		for (Entry<String, Map<String, Map<?, ?>>> map : devices.entrySet()) {
			this.devices.put(map.getKey(), new HashMap<>());
			for (Entry<String, Map<?, ?>> subMap : map.getValue().entrySet()) {
				Device d = new Device();
				d.setName(subMap.getValue().get("name").toString());
				Map<?, ?> data = (Map<?, ?>) subMap.getValue().get("data");
				for (Entry<?, ?> dataMap : data.entrySet()) {
					ASduType aSduType = ASduType.valueOf(dataMap.getKey().toString());
					d.getData().put(aSduType, new HashMap<>());
					for (Entry<?, ?> dataSubMap : ((Map<?, ?>) dataMap.getValue()).entrySet()) {
						String ioa = dataSubMap.getKey().toString();
						d.getData().get(aSduType).put(ioa, new HashMap<>());
						for (Entry<?, ?> dataSubSubMap : ((Map<?, ?>) dataSubMap.getValue()).entrySet()) {
							DataType dataType = DataType.valueOf(dataSubSubMap.getKey().toString());
							d.getData().get(aSduType).get(ioa).put(dataType, new HashMap<>());
							for (Entry<?, ?> dataSubSubSubMap : ((Map<?, ?>) dataSubSubMap.getValue()).entrySet()) {
								d.getData().get(aSduType).get(ioa).get(dataType).put(
										dataSubSubSubMap.getKey().toString(), dataSubSubSubMap.getValue().toString());
							}
						}
					}
				}
				this.devices.get(map.getKey()).put(subMap.getKey(), d);
			}
		}
	}

}
