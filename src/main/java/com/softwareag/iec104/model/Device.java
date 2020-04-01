package com.softwareag.iec104.model;

import java.util.HashMap;
import java.util.Map;

import org.openmuc.j60870.ASduType;

import com.softwareag.iec104.IEC104Client.DataType;

public class Device {
	private String name;
	private Map<ASduType, Map<String, Map<DataType, Map<String,String>>>> data = new HashMap<>();

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Map<ASduType, Map<String, Map<DataType, Map<String,String>>>> getData() {
		return data;
	}
	public void setData(Map<ASduType, Map<String, Map<DataType, Map<String,String>>>> data) {
		this.data = data;
	}
}
