package com.laundryhub.backend.service;

import com.laundryhub.backend.domain.user.AccountStatus;
import com.laundryhub.backend.domain.user.ConsoleUser;
import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.domain.user.User;
import com.laundryhub.backend.exception.ForbiddenException;
import com.laundryhub.backend.exception.UnauthorizedException;
import com.laundryhub.backend.repository.UserRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the signed-in account from the token's user id and checks it is still active and has
 * the expected role. A token for a deactivated account stops working at once.
 */
@Component
public class AccountLookup {

    private final UserRepository users;

    /**
     * @param users account repository
     */
    public AccountLookup(UserRepository users) {
        this.users = users;
    }

    /**
     * @param userId id from the token
     * @param type   expected account class, e.g. {@code Rider.class}
     * @param <T>    account type
     * @return the active account
     * @throws UnauthorizedException if the account is gone or inactive
     * @throws ForbiddenException    if the account has another role
     */
    @Transactional(readOnly = true)
    public <T extends User> T requireActive(Long userId, Class<T> type) {
        User user = users.findById(userId).orElseThrow(() -> new UnauthorizedException("Please sign in again."));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("This account has been deactivated.");
        }
        if (!type.isInstance(user)) {
            throw new ForbiddenException("This action is not available for your account.");
        }
        return type.cast(user);
    }

    /**
     * @param userId id from the token
     * @return the active customer
     */
    public Customer customer(Long userId) {
        return requireActive(userId, Customer.class);
    }

    /**
     * @param userId id from the token
     * @return the active rider
     */
    public Rider rider(Long userId) {
        return requireActive(userId, Rider.class);
    }

    /**
     * @param userId id from the token
     * @return the active admin or staff member
     */
    public ConsoleUser consoleUser(Long userId) {
        return requireActive(userId, ConsoleUser.class);
    }

    /**
     * @param userId id from the token
     * @return the active account of any role
     */
    public User anyRole(Long userId) {
        return requireActive(userId, User.class);
    }
}
