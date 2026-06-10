package com.microservices.brokermessage.controller;

import com.microservices.brokermessage.model.Envio;
import com.microservices.brokermessage.repository.EnvioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ordenes/envios")
public class EnvioController {

    @Autowired
    private EnvioRepository envioRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getEnvios() {
        List<Envio> envios = envioRepository.findAll();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 200);
        response.put("data", envios);
        return ResponseEntity.ok(response);
    }
}
