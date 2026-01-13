package com.radioacademy.backend.event;

import com.radioacademy.backend.entity.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PasswordResetEvent extends ApplicationEvent {
    private final User user;
    private final String token;

    public PasswordResetEvent(Object source, User user, String token) {
        super(source);
        this.user = user;
        this.token = token;
    }
}