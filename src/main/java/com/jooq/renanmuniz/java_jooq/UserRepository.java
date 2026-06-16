package com.jooq.renanmuniz.java_jooq;

import com.jooq.renanmuniz.java_jooq.public_.tables.records.UsersRecord;

public interface UserRepository {
    UsersRecord findByUserName(String username);

    UsersRecord create(CreateUserRequest request);

    UsersRecord delete(String username);
}
