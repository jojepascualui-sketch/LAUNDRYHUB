package com.laundryhub.backend.web.dto;

import com.laundryhub.backend.domain.user.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A4 "Add customer" / "Add rider" / "Add staff".
 *
 * @param role         account role
 * @param fullName     name
 * @param email        e-mail (sign-in)
 * @param mobileNumber contact, optional
 * @param password     initial password, optional (without one the account cannot sign in yet)
 * @param plateNumber  vehicle plate, required for riders
 * @param position     role label, required for admin/staff, e.g. "Counter staff"
 */
public record UserCreateRequest(
        @NotNull Role role,
        @NotBlank @Size(max = 120) String fullName,
        @NotBlank @Email @Size(max = 190) String email,
        @Pattern(regexp = ApiValidation.MOBILE_REGEX, message = ApiValidation.MOBILE_MESSAGE) String mobileNumber,
        @Size(min = ApiValidation.MIN_PASSWORD_LENGTH, max = ApiValidation.MAX_PASSWORD_LENGTH) String password,
        @Size(max = 20) String plateNumber,
        @Size(max = 60) String position) {
}
