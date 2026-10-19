package com.laundryhub.backend.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.laundryhub.backend.testsupport.Fixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** User hierarchy: validation in the constructors, roles and console permissions (polymorphism). */
class UserTest {

    /** Lowest BCrypt cost, to keep the test fast. */
    private static final int FAST_BCRYPT_COST = 4;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(FAST_BCRYPT_COST);

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankNamesAreRejected(String name) {
        assertThatThrownBy(() -> new Customer(name, "ana@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full name must not be blank");
    }

    @Test
    void tooLongNamesAreRejected() {
        String longName = "x".repeat(User.MAX_NAME_LENGTH + 1);

        assertThatThrownBy(() -> new Customer(longName, "ana@example.com", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Full name must be at most 120 characters");
    }

    @Test
    void emailIsValidatedAndNormalised() {
        assertThat(new Customer("Ana", " ANA@Example.com ", null).getEmail()).isEqualTo("ana@example.com");
        assertThatThrownBy(() -> new Customer("Ana", "not-an-email", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is not a valid address");
    }

    @Test
    void mobileIsOptionalButMustBeValid() {
        assertThat(new Customer("Ana", "ana@example.com", null).getMobileNumber()).isNull();
        assertThatThrownBy(() -> new Customer("Ana", "ana@example.com", "12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mobile number must look like 09XXXXXXXXX");
    }

    @Test
    void riderNeedsAPlateNumberStoredInCapitals() {
        assertThatThrownBy(() -> new Rider("Carlo", "carlo@example.com", "09190001234", " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Plate number must not be blank");
        assertThat(new Rider("Carlo", "carlo@example.com", "09190001234", " abc 1234 ").getPlateNumber())
                .isEqualTo("ABC 1234");
    }

    @Test
    void riderStartsOffDutyAndTogglesDuty() {
        Rider rider = Fixtures.rider("rider1@example.com", false);
        assertThat(rider.isOnDuty()).isFalse();

        rider.goOnDuty();
        assertThat(rider.isOnDuty()).isTrue();
        rider.goOffDuty();
        assertThat(rider.isOnDuty()).isFalse();
    }

    @Test
    void consoleUsersNeedAPosition() {
        assertThatThrownBy(() -> new Staff("Liza", "liza@laundryhub.test", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Position must not be blank");
    }

    @Test
    void eachSubclassAnswersWithItsOwnRole() {
        List<User> users = List.of(Fixtures.customer(), Fixtures.rider("r@example.com", false), Fixtures.admin(),
                Fixtures.staff());

        assertThat(users).extracting(User::getRole)
                .containsExactly(Role.CUSTOMER, Role.RIDER, Role.ADMIN, Role.STAFF);
        assertThat(users).extracting(User::canUseAdminConsole).containsExactly(false, false, true, true);
    }

    @Test
    void onlyTheAdminManagesPricesAndAccounts() {
        List<ConsoleUser> console = List.of(Fixtures.admin(), Fixtures.staff());

        assertThat(console).extracting(ConsoleUser::canManagePricing).containsExactly(true, false);
        assertThat(console).extracting(ConsoleUser::canManageAccounts).containsExactly(true, false);
    }

    @Test
    void passwordMustBeABcryptHash() {
        Customer customer = Fixtures.customer();

        assertThatThrownBy(() -> customer.changePasswordHash("plain-text-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password must be stored as a BCrypt hash");
        assertThatThrownBy(() -> customer.changePasswordHash(null)).isInstanceOf(IllegalArgumentException.class);
        customer.changePasswordHash(encoder.encode("secret-pass-1"));
        assertThat(encoder.matches("secret-pass-1", customer.getPasswordHash())).isTrue();
    }

    @Test
    void onlyActiveAccountsWithAPasswordCanSignIn() {
        Customer customer = Fixtures.customer();
        assertThat(customer.canSignIn()).isFalse(); // e.g. created by Google sign-in

        customer.changePasswordHash(encoder.encode("secret-pass-1"));
        assertThat(customer.canSignIn()).isTrue();

        customer.deactivate();
        assertThat(customer.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(customer.canSignIn()).isFalse();

        customer.activate();
        assertThat(customer.canSignIn()).isTrue();
    }

    @Test
    void initialsUseTheFirstTwoWords() {
        assertThat(new Customer("Juan Dela Cruz", "juan@example.com", null).getInitials()).isEqualTo("JD");
        assertThat(new Customer("  maria  ", "maria@example.com", null).getInitials()).isEqualTo("M");
    }

    @Test
    void renameValidatesToo() {
        Customer customer = Fixtures.customer();

        customer.rename("  Ana R. Reyes ");
        assertThat(customer.getFullName()).isEqualTo("Ana R. Reyes");
        assertThatThrownBy(() -> customer.rename(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThat(customer.getFullName()).isEqualTo("Ana R. Reyes");
    }
}
