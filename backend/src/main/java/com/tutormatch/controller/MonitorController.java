package com.tutormatch.controller;

import com.tutormatch.dto.admin.MonitorDtos.Snapshot;
import com.tutormatch.service.MonitorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only (see SecurityConfig: /api/admin/**). */
@RestController
@RequestMapping("/api/admin")
public class MonitorController {
  private final MonitorService monitor;

  public MonitorController(MonitorService monitor) {
    this.monitor = monitor;
  }

  @GetMapping("/monitor")
  public Snapshot monitor() {
    return monitor.snapshot();
  }
}
