package com.jooq.renanmuniz.java_jooq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static com.jooq.renanmuniz.java_jooq.UserMapper.toUserDTO;

@RestController
@RequestMapping("/users")
public class UserController {
    private final UserRepository userRepository;

    @Autowired
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("{userName}")
    public UserDTO getUser(@PathVariable String userName) {
        var userRecord = userRepository.findByUserName(userName);
        if(userRecord == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userName);
        }

        return toUserDTO(userRecord);
    }

    @PostMapping
    public UserDTO createUser(@RequestBody CreateUserRequest request) {
        try {
            var userRecord = userRepository.create(request);
            return toUserDTO(userRecord);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username already taken: " + request.userName());
        }
    }

    @DeleteMapping("{userName}")
    public UserDTO deleteUser(@PathVariable String userName) {
        try {
            var deletedRecord = userRepository.delete(userName);
            return toUserDTO(deletedRecord);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + userName);
        }
    }

}
