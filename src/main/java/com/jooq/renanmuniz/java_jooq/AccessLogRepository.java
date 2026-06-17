package com.jooq.renanmuniz.java_jooq;

import com.jooq.renanmuniz.java_jooq.public_.tables.records.AccessLogRecord;

import java.util.List;

public interface AccessLogRepository {
    List<AccessLogDTO> findByUserName(String userName);

    AccessLogRecord create(CreateAccessLogRequest request);
}
