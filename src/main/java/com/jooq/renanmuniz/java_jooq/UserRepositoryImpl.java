package com.jooq.renanmuniz.java_jooq;

import com.jooq.renanmuniz.java_jooq.public_.tables.Users;
import com.jooq.renanmuniz.java_jooq.public_.tables.records.UsersRecord;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepository {
    private final DSLContext ctx;

    public UserRepositoryImpl(DSLContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public UsersRecord findByUserName(String username) {
        return ctx.selectFrom(Users.USERS)
                .where(Users.USERS.USERNAME.eq(username))
                .fetchOne();
    }

    @Override
    public UsersRecord create(CreateUserRequest request) {
        var usersRecord = ctx.newRecord(Users.USERS);
        usersRecord.setUsername(request.userName());
        usersRecord.setEmail(request.email());
        usersRecord.setPassword(request.password());
        int stored = usersRecord.store();

        if(stored != 1) {
            throw new RuntimeException("Failed to create user");
        }

        return usersRecord;
    }

    @Override
    public UsersRecord delete(String username) {
        UsersRecord deleted = ctx.deleteFrom(Users.USERS)
                .where(Users.USERS.USERNAME.eq(username))
                .returning()
                .fetchOne();

        if (deleted == null) {
            throw new RuntimeException("User not found: " + username);
        }

        return deleted;
    }
}
