package com.jooq.renanmuniz.java_jooq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/access")
public class AccessLogController {
    private final AccessLogRepository accessLogRepository;

    @Autowired
    public AccessLogController(AccessLogRepository accessLogRepository) {
        this.accessLogRepository = accessLogRepository;
    }

    @GetMapping("{userName}")
    public List<AccessLogDTO> getAccessLogs(@PathVariable String userName) {
        var accessLogDTOs = accessLogRepository.findByUserName(userName);
        if (accessLogDTOs.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No access logs found for user: " + userName);
        }
        return accessLogDTOs;
    }

    @PostMapping
    public AccessLogDTO createAccessLog(@RequestBody CreateAccessLogRequest request) {
        var record = accessLogRepository.create(request);
        return new AccessLogDTO(record.getUserId(), request.userName(), record.getLoggedInAt());
    }

}
