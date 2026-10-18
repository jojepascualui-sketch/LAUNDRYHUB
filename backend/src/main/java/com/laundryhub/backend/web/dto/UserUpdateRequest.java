package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.user.AccountStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A4 "Edit". The e-mail cannot change.
 *
 * @param fullName     name
 * @param mobileNumber contact, or {@code null} to clear
 * @param status       ACTIVE or INACTIVE
 * @param plateNumber  vehicle plate (riders; ignored otherwise)
 * @param position     role label (admin/staff; ignored otherwise)
 * @param password     new password, or {@code null} to keep it
 */
public record UserUpdateRequest(
        @NotBlank @Size(max = 120) String fullName,
        @Pattern(regexp = ApiValidation.MOBILE_REGEX, message = ApiValidation.MOBILE_MESSAGE) String mobileNumber,
        @NotNull AccountStatus status,
        @Size(max = 20) String plateNumber,
        @Size(max = 60) String position,
        @Size(min = ApiValidation.MIN_PASSWORD_LENGTH, max = ApiValidation.MAX_PASSWORD_LENGTH) String password) {
}
