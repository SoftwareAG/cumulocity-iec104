package com.softwareag.iec104.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.softwareag.iec104.configuration.GatewayConfigurationProperties;

@RestController
public class ServiceEndpoint {
	
	@Autowired
	private GatewayConfigurationProperties properties;

    @GetMapping(value = "/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    @PostMapping(value = "/config")
    public ResponseEntity<String> lnsUp(@RequestBody String config) {
    	properties.updateConfig(config);
        return ResponseEntity.ok().build();
    }

}
