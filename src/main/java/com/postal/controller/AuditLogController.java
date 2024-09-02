package com.postal.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.postal.service.FabricClientService;

@RestController
@RequestMapping("/api/audit")
public class AuditLogController {

    private static final Logger logger = LogManager.getLogger(AuditLogController.class);

    @Autowired
    private FabricClientService fabricClientService;

    @GetMapping("/queryAllLogs")
    public ResponseEntity<String> queryAllLogs() {
        try {
            String logs = fabricClientService.queryAllAuditLogs();
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            logger.error("Error querying all logs: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error querying all logs: " + e.getMessage());
        }
    }

    @GetMapping("/getHistory")
    public ResponseEntity<String> getHistory(@RequestParam String parcelId) {
        try {
            String history = fabricClientService.queryAuditLogHistory(parcelId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Error querying audit log history: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Error querying audit log history: " + e.getMessage());
        }
    }
}
