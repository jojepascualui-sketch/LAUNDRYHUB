package com.laundryhub.backend.domain.user;

import com.laundryhub.backend.domain.common.AuditedEntity;
import com.laundryhub.backend.domain.common.Check;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Any account that can use Laundry HUB. Subclasses: {@link Customer}, {@link Rider},
 * {@link Admin} and {@link Staff}; the {@code role} column decides which one a row is.
 */
// OOP: Abstraction — code works with "a user" and asks it what it may do, without knowing the role.
// OOP: Inheritance — the subclasses share name, e-mail, phone, password and status.
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING, length = 20)
public abstract class User extends AuditedEntity {

    /** Longest full name accepted. */
    public static final int MAX_NAME_LENGTH = 120;

    /** Longest e-mail address accepted. */
    public static final int MAX_EMAIL_LENGTH = 190;

    private static final String BCRYPT_PREFIX = "$2";
    private static final int BCRYPT_HASH_LENGTH = 60;

    // OOP: Encapsulation — private fields; changes go through validating methods.
    @Column(name = "full_name", nullable = false, length = MAX_NAME_LENGTH)
    private String fullName;

    @Column(name = "email", nullable = false, length = MAX_EMAIL_LENGTH, unique = true)
    private String email;

    @Column(name = "mobile_number", length = Check.MOBILE_LENGTH)
    private String mobileNumber;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    /** For JPA only. */
    protected User() {
    }

    /**
     * Creates an active account without a password.
     *
     * @param fullName     full name, e.g. "Juan Dela Cruz"
     * @param email        e-mail address (stored lower-case)
     * @param mobileNumber mobile number 09XXXXXXXXX, or {@code null}
     * @throws IllegalArgumentException if a value is missing or malformed
     */
    protected User(String fullName, String email, String mobileNumber) {
        rename(fullName);
        this.email = Check.email(email, MAX_EMAIL_LENGTH);
        changeMobileNumber(mobileNumber);
    }

    /** @return the role stored in the {@code role} column */
    // OOP: Polymorphism — each subclass answers with its own role.
    public abstract Role getRole();

    /** @return whether this account may sign in to the JavaFX admin console */
    /**
     * @return the side this account writes on in the feedback chat: customers and riders own a conversation
     *         (USER); console users answer for the shop (SHOP)
     */
    // OOP: Polymorphism — ConsoleUser overrides this to SHOP.
    public com.laundryhub.backend.domain.feedback.MessageSide chatSide() {
        return com.laundryhub.backend.domain.feedback.MessageSide.USER;
    }

    // OOP: Polymorphism — only console users override this to {@code true}.
    public boolean canUseAdminConsole() {
        return false;
    }

    /**
     * Changes the full name.
     *
     * @param fullName new full name
     */
    public final void rename(String fullName) {
        this.fullName = Check.text(fullName, "Full name", MAX_NAME_LENGTH);
    }

    /**
     * Changes the mobile number.
     *
     * @param mobileNumber 09XXXXXXXXX, or {@code null} to clear it
     */
    public final void changeMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber == null ? null : Check.mobileNumber(mobileNumber, "Mobile number");
    }

    /**
     * Stores a new password hash. Hashing is done by the caller (BCrypt).
     *
     * @param bcryptHash a BCrypt hash, never a plain password
     * @throws IllegalArgumentException if the value is not a BCrypt hash
     */
    public void changePasswordHash(String bcryptHash) {
        if (bcryptHash == null || !bcryptHash.startsWith(BCRYPT_PREFIX) || bcryptHash.length() != BCRYPT_HASH_LENGTH) {
            throw new IllegalArgumentException("Password must be stored as a BCrypt hash");
        }
        this.passwordHash = bcryptHash;
    }

    /** Lets the account sign in again. */
    public void activate() {
        status = AccountStatus.ACTIVE;
    }

    /** Blocks the account from signing in. */
    public void deactivate() {
        status = AccountStatus.INACTIVE;
    }

    /** @return whether the account is active and has a password */
    public boolean canSignIn() {
        return status == AccountStatus.ACTIVE && passwordHash != null;
    }

    /** @return full name */
    public String getFullName() {
        return fullName;
    }

    /** @return e-mail address, lower-case */
    public String getEmail() {
        return email;
    }

    /** @return mobile number, or {@code null} */
    public String getMobileNumber() {
        return mobileNumber;
    }

    /** @return BCrypt password hash, or {@code null} if no password is set */
    public String getPasswordHash() {
        return passwordHash;
    }

    /** @return account status */
    public AccountStatus getStatus() {
        return status;
    }

    /** @return initials for avatars, e.g. "AR" for "Ana Reyes" */
    public String getInitials() {
        StringBuilder initials = new StringBuilder();
        for (String part : fullName.split("\\s+")) {
            if (!part.isEmpty() && initials.length() < 2) {
                initials.append(Character.toUpperCase(part.charAt(0)));
            }
        }
        return initials.toString();
    }
}
