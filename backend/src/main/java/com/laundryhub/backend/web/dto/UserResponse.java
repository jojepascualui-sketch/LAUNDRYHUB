package com.laundryhub.backend.web.dto;

import java.time.LocalDateTime;

import com.laundryhub.backend.domain.user.AccountStatus;
import com.laundryhub.backend.domain.user.ConsoleUser;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.domain.user.Role;
import com.laundryhub.backend.domain.user.User;

/**
 * A4 table row. The third column is Orders (customers), Vehicle (riders) or Role (staff).
 *
 * @param id           user id
 * @param role         role
 * @param fullName     "Name"
 * @param email        e-mail
 * @param mobileNumber "Contact"
 * @param initials     avatar initials
 * @param status       "Status" badge
 * @param orderCount   orders placed (customers), else {@code null}
 * @param plateNumber  vehicle (riders), else {@code null}
 * @param onDuty       duty switch (riders), else {@code null}
 * @param position     role label (admin/staff), else {@code null}
 * @param createdAt    when created
 */
public record UserResponse(Long id, Role role, String fullName, String email, String mobileNumber, String initials,
                           AccountStatus status, Long orderCount, String plateNumber, Boolean onDuty, String position,
                           LocalDateTime createdAt) {

    /**
     * @param user       an account
     * @param orderCount orders placed (customers only; ignored otherwise)
     * @return the row
     */
    public static UserResponse from(User user, long orderCount) {
        Rider rider = user instanceof Rider r ? r : null;
        String position = user instanceof ConsoleUser console ? console.getPosition() : null;
        return new UserResponse(user.getId(), user.getRole(), user.getFullName(), user.getEmail(),
                user.getMobileNumber(), user.getInitials(), user.getStatus(),
                user.getRole() == Role.CUSTOMER ? orderCount : null,
                rider == null ? null : rider.getPlateNumber(), rider == null ? null : rider.isOnDuty(), position,
                user.getCreatedAt());
    }
}
