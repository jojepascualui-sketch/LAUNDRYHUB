package com.laundryhub.backend.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Marks a {@code Long} controller parameter that receives the signed-in user's id
 * (the JWT subject). Resolved by {@link CurrentUserIdResolver}; hidden from Swagger.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Parameter(hidden = true)
public @interface CurrentUserId {
}
