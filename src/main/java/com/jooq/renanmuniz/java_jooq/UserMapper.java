package com.jooq.renanmuniz.java_jooq;

import com.jooq.renanmuniz.java_jooq.public_.tables.records.UsersRecord;

public class UserMapper {

    public static UserDTO toUserDTO(UsersRecord user) {
        return new UserDTO(
                user.getUsername(),
                user.getEmail()
        );
    }

}
