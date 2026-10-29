package com.example.taskmanager.controller;

import com.example.taskmanager.dto.ActivityLogResponse;
import com.example.taskmanager.service.ActivityLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/admin/activity")
public class ActivityLogController {
    private final ActivityLogService service;

    public ActivityLogController(ActivityLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivityLogResponse> recent() {
        return service.recent();
    }
}
