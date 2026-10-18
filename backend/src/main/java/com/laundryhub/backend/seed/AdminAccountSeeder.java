package com.laundryhub.backend.seed;

import com.laundryhub.backend.domain.user.Admin;
import com.laundryhub.backend.repository.AdminRepository;
import com.laundryhub.backend.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the first admin account on startup when none exists. The password comes from the
 * {@code ADMIN_DEFAULT_PASSWORD} environment variable and is stored BCrypt-hashed only.
 */
@Component
public class AdminAccountSeeder implements ApplicationRunner {

    /** Shortest password accepted ("At least 8 characters" on the sign-in screen). */
    public static final int MIN_PASSWORD_LENGTH = 8;

    /** Name of the first admin, as on the A1 sidebar. */
    public static final String DEFAULT_ADMIN_NAME = "Admin";

    private static final Logger LOG = LoggerFactory.getLogger(AdminAccountSeeder.class);

    private final AdminRepository admins;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String defaultPassword;

    /**
     * @param admins          admin repository
     * @param users           user repository (to check the e-mail is free)
     * @param passwordEncoder BCrypt encoder
     * @param email           admin e-mail ({@code ADMIN_DEFAULT_EMAIL}, default admin@laundryhub.ph)
     * @param defaultPassword plain password from {@code ADMIN_DEFAULT_PASSWORD}; never logged
     */
    public AdminAccountSeeder(AdminRepository admins, UserRepository users, PasswordEncoder passwordEncoder,
                              @Value("${laundryhub.admin.email}") String email,
                              @Value("${laundryhub.admin.default-password}") String defaultPassword) {
        this.admins = admins;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.defaultPassword = defaultPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (admins.count() > 0) {
            return;
        }
        if (defaultPassword == null || defaultPassword.isBlank()) {
            LOG.warn("No admin account exists. Set ADMIN_DEFAULT_PASSWORD and restart to create {}", email);
            return;
        }
        if (defaultPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_DEFAULT_PASSWORD must be at least " + MIN_PASSWORD_LENGTH
                    + " characters");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException(email + " is already used by a non-admin account");
        }
        Admin admin = new Admin(DEFAULT_ADMIN_NAME, email, Admin.DEFAULT_POSITION);
        admin.changePasswordHash(passwordEncoder.encode(defaultPassword));
        admins.save(admin);
        LOG.info("Created admin account {}", email);
    }
}
