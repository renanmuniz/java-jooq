package com.jooq.renanmuniz.java_jooq;

import java.time.OffsetDateTime;

public record AccessLogDTO(int userId, String userName, OffsetDateTime accessTime) {
}
