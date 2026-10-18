package com.laundryhub.backend.repository;

import java.util.List;
import java.util.Optional;

import com.laundryhub.backend.domain.user.User;

import org.springframework.data.jpa.repository.JpaRepository;

/** Every account, whatever its role (sign-in). */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * @param email e-mail address
     * @return the account with that e-mail, ignoring case
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * @param email e-mail address
     * @return whether an account already uses that e-mail
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * @param mobileNumber 09XXXXXXXXX
     * @return accounts with that number (mobile numbers are not unique)
     */
    List<User> findByMobileNumber(String mobileNumber);
}
