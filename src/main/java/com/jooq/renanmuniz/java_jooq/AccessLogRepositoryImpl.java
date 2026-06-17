package com.jooq.renanmuniz.java_jooq;

import com.jooq.renanmuniz.java_jooq.public_.tables.AccessLog;
import com.jooq.renanmuniz.java_jooq.public_.tables.Users;
import com.jooq.renanmuniz.java_jooq.public_.tables.records.AccessLogRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class AccessLogRepositoryImpl implements AccessLogRepository {
    private final DSLContext ctx;

    public AccessLogRepositoryImpl(DSLContext ctx) {
        this.ctx = ctx;
    }



    @Override
    public List<AccessLogDTO> findByUserName(String userName) {
        return ctx.select(
                        AccessLog.ACCESS_LOG.USER_ID,
                        Users.USERS.USERNAME,
                        AccessLog.ACCESS_LOG.LOGGED_IN_AT
                )
                .from(AccessLog.ACCESS_LOG)
                .join(Users.USERS).on(AccessLog.ACCESS_LOG.USER_ID.eq(Users.USERS.ID))
                .where(Users.USERS.USERNAME.eq(userName))
                .orderBy(AccessLog.ACCESS_LOG.LOGGED_IN_AT.desc())
                .fetch(r -> new AccessLogDTO(
                        r.get(AccessLog.ACCESS_LOG.USER_ID),
                        r.get(Users.USERS.USERNAME),
                        r.get(AccessLog.ACCESS_LOG.LOGGED_IN_AT)
                ));
    }

    @Override
    public AccessLogRecord create(CreateAccessLogRequest request) {
        var user = ctx.selectFrom(Users.USERS)
                .where(Users.USERS.USERNAME.eq(request.userName()))
                .fetchOne();

        if (user == null) {
            throw new RuntimeException("User not found: " + request.userName());
        }

        var accessLogRecord = ctx.newRecord(AccessLog.ACCESS_LOG);
        accessLogRecord.setUserId(user.getId());
        accessLogRecord.setLoggedInAt(OffsetDateTime.now());

        int stored = accessLogRecord.store();

        if(stored != 1) {
            throw new RuntimeException("Failed to save access log");
        }

        return accessLogRecord;
    }

    //TODO [X] Create another table named `access_log`. It must store the username and logging timestamp
    //TODO [X] Create the repository for the access_log table
    //TODO [X] Create and endpoint that saves this logging information(id, logged_in_at).
    //TODO [X] Create and endpoint to retrieve this logging info. But, I want to practice a JOIN query. So return the username and the timestamp.

}
