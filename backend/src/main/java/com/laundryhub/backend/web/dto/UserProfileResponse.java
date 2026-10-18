package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.user.ConsoleUser;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.domain.user.Role;
import com.laundryhub.backend.domain.user.User;

/**
 * The signed-in account: "Hi, [RIDER NAME]" (R1), the admin sidebar profile (A1), routing after sign-in.
 *
 * @param id               user id
 * @param role             role
 * @param fullName         full name
 * @param email            e-mail
 * @param mobileNumber     mobile number, or {@code null}
 * @param initials         avatar initials
 * @param position         console position, e.g. "Owner" (admin/staff only)
 * @param plateNumber      plate number (riders only)
 * @param onDuty           duty switch (riders only)
 * @param usesAdminConsole whether this account belongs in the JavaFX console (00c routes it there)
 */
public record UserProfileResponse(Long id, Role role, String fullName, String email, String mobileNumber,
                                  String initials, String position, String plateNumber, Boolean onDuty,
                                  boolean usesAdminConsole) {

    /**
     * @param user an account
     * @return its profile
     */
    public static UserProfileResponse from(User user) {
        String position = user instanceof ConsoleUser console ? console.getPosition() : null;
        Rider rider = user instanceof Rider r ? r : null;
        return new UserProfileResponse(user.getId(), user.getRole(), user.getFullName(), user.getEmail(),
                user.getMobileNumber(), user.getInitials(), position,
                rider == null ? null : rider.getPlateNumber(), rider == null ? null : rider.isOnDuty(),
                user.canUseAdminConsole());
    }
}
