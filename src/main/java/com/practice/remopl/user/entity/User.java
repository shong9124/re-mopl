package com.practice.remopl.user.entity;

import com.practice.remopl.global.entity.BaseUpdatableEntity;
import com.practice.remopl.user.exception.InvalidUsernameException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                )
        }
)
@NoArgsConstructor
public class User extends BaseUpdatableEntity {

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "profile_image_id", length = 512)
    private UUID profileImageId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Column(nullable = false)
    private boolean locked;

    public static User create(String email, String password, String name) {
        return new User(email, password, name);
    }

    private User(String email, String password, String name) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.role = UserRole.USER;
        this.locked = false;
        this.profileImageId = null;
    }

    private void updateName(String newName) {
        if (newName.isBlank() || newName == null) {
            throw new InvalidUsernameException();
        }
        this.name = newName;
    }
}
