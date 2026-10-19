package com.laundryhub.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.user.AccountStatus;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.domain.user.Role;
import com.laundryhub.backend.domain.user.User;
import com.laundryhub.backend.exception.ConflictException;
import com.laundryhub.backend.exception.ForbiddenException;
import com.laundryhub.backend.exception.NotFoundException;
import com.laundryhub.backend.repository.AdminRepository;
import com.laundryhub.backend.repository.CustomerRepository;
import com.laundryhub.backend.repository.OrderRepository;
import com.laundryhub.backend.repository.PricingSettingsRepository;
import com.laundryhub.backend.repository.RiderRepository;
import com.laundryhub.backend.repository.SoapRepository;
import com.laundryhub.backend.repository.StaffRepository;
import com.laundryhub.backend.repository.UserRepository;
import com.laundryhub.backend.testsupport.Fixtures;
import com.laundryhub.backend.web.dto.PricingUpdateRequest;
import com.laundryhub.backend.web.dto.SoapRequest;
import com.laundryhub.backend.web.dto.SoapResponse;
import com.laundryhub.backend.web.dto.UserCreateRequest;
import com.laundryhub.backend.web.dto.UserResponse;
import com.laundryhub.backend.web.dto.UserUpdateRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** A3 (AdminCatalogService) and A4 (AdminUserService): permission checks and validation. */
@ExtendWith(MockitoExtension.class)
class AdminServicesTest {

    private static final int FAST_BCRYPT_COST = 4;
    private static final long ADMIN_ID = 1L;
    private static final long STAFF_ID = 2L;
    private static final long OTHER_ID = 3L;
    private static final long SOAP_ID = 5L;
    private static final String PASSWORD = "rider-pass-1";

    @Mock
    private PricingSettingsRepository pricing;
    @Mock
    private SoapRepository soaps;
    @Mock
    private OrderRepository orders;
    @Mock
    private AccountLookup accounts;
    @Mock
    private UserRepository users;
    @Mock
    private CustomerRepository customers;
    @Mock
    private RiderRepository riders;
    @Mock
    private StaffRepository staff;
    @Mock
    private AdminRepository admins;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(FAST_BCRYPT_COST);
    private final PricingSettings settings = PricingSettings.defaults();
    private AdminCatalogService catalog;
    private AdminUserService accountsAdmin;

    @BeforeEach
    void setUp() {
        catalog = new AdminCatalogService(pricing, soaps, orders, accounts);
        accountsAdmin = new AdminUserService(users, customers, riders, staff, admins, orders, encoder, accounts);
        lenient().when(accounts.consoleUser(ADMIN_ID)).thenReturn(Fixtures.admin());
        lenient().when(accounts.consoleUser(STAFF_ID)).thenReturn(Fixtures.staff());
        lenient().when(pricing.lockSettings()).thenReturn(settings);
        lenient().when(soaps.save(any(Soap.class))).thenAnswer(call -> call.getArgument(0));
        lenient().when(users.save(any(User.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static PricingUpdateRequest pricingForm(String shopGcash) {
        return new PricingUpdateRequest(new BigDecimal("35"), new BigDecimal("6"), new BigDecimal("50"),
                new BigDecimal("3"), true, true, shopGcash);
    }

    private static SoapRequest soapForm(String name, String addOn, String tint, Integer sortOrder) {
        return new SoapRequest(name, "Plant-based formula", new BigDecimal(addOn), true, tint, sortOrder);
    }

    // ---- AdminCatalogService ----------------------------------------------------------------

    @Test
    void adminUpdatesPricingAndBlankShopNumberCountsAsMissing() {
        assertThatThrownBy(() -> catalog.updatePricing(ADMIN_ID, pricingForm("   ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Shop GCash number is required when GCash is enabled");

        catalog.updatePricing(ADMIN_ID, pricingForm(" 09170009999 "));

        assertThat(settings.getRatePerKg()).isEqualTo(new BigDecimal("35.00"));
        assertThat(settings.getShopGcashNumber()).isEqualTo("09170009999");
        verify(pricing).flush();
    }

    @Test
    void staffCannotChangePricing() {
        assertThatThrownBy(() -> catalog.updatePricing(STAFF_ID, pricingForm("09170009999")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(settings.getRatePerKg()).isEqualByComparingTo("30");
    }

    @Test
    void newSoapGoesLastWithTheDefaultTint() {
        when(soaps.findFirstByOrderBySortOrderDesc()).thenReturn(Optional.of(Fixtures.babySafe()));

        SoapResponse created = catalog.createSoap(ADMIN_ID, soapForm(" Eco Clean ", "4", null, null));

        assertThat(created.name()).isEqualTo("Eco Clean");
        assertThat(created.sortOrder()).isEqualTo(5);
        assertThat(created.tintHex()).isEqualTo(Soap.DEFAULT_TINT);
        assertThat(created.addOnPerKg()).isEqualTo(new BigDecimal("4.00"));
    }

    @Test
    void firstSoapGetsSortOrderOne() {
        when(soaps.findFirstByOrderBySortOrderDesc()).thenReturn(Optional.empty());

        assertThat(catalog.createSoap(ADMIN_ID, soapForm("Eco", "0", "#abcdef", null)).sortOrder()).isEqualTo(1);
    }

    @Test
    void soapNamesAreUnique() {
        when(soaps.existsByNameIgnoreCase("Standard")).thenReturn(true);
        Soap eco = Fixtures.premiumScented();
        when(soaps.findById(SOAP_ID)).thenReturn(Optional.of(eco));
        when(soaps.existsByNameIgnoreCaseAndIdNot("Standard", SOAP_ID)).thenReturn(true);

        assertThatThrownBy(() -> catalog.createSoap(ADMIN_ID, soapForm(" Standard", "0", null, 1)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("A soap named \"Standard\" already exists.");
        assertThatThrownBy(() -> catalog.updateSoap(ADMIN_ID, SOAP_ID, soapForm("Standard", "0", null, null)))
                .isInstanceOf(ConflictException.class);
        verify(soaps, never()).save(any());
        assertThat(eco.getName()).isEqualTo("Premium Scented");
    }

    @Test
    void negativeAddOnIsRejectedByTheDomain() {
        when(soaps.findFirstByOrderBySortOrderDesc()).thenReturn(Optional.empty());

        assertThatThrownBy(() -> catalog.createSoap(ADMIN_ID, soapForm("Eco", "-1", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Add-on per kg must not be negative");
    }

    @Test
    void updateKeepsTheTintWhenNoneIsSent() {
        Soap soap = Fixtures.hypoallergenic();
        when(soaps.findById(SOAP_ID)).thenReturn(Optional.of(soap));

        SoapResponse updated = catalog.updateSoap(ADMIN_ID, SOAP_ID,
                new SoapRequest("Hypoallergenic", "Gentle", new BigDecimal("5.5"), false, null, null));

        assertThat(updated.tintHex()).isEqualTo("#E6F2EA");
        assertThat(updated.available()).isFalse();
        assertThat(updated.addOnPerKg()).isEqualTo(new BigDecimal("5.50"));
        assertThat(updated.sortOrder()).isEqualTo(2);
    }

    @Test
    void aSoapUsedByOrdersCannotBeDeleted() {
        Soap soap = Fixtures.babySafe();
        when(soaps.findById(SOAP_ID)).thenReturn(Optional.of(soap));
        when(orders.existsBySoapId(SOAP_ID)).thenReturn(true);

        assertThatThrownBy(() -> catalog.deleteSoap(ADMIN_ID, SOAP_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Baby-Safe is used by orders. Untick \"Available\" instead.");
        verify(soaps, never()).delete(any());

        when(orders.existsBySoapId(SOAP_ID)).thenReturn(false);
        catalog.deleteSoap(ADMIN_ID, SOAP_ID);
        verify(soaps).delete(soap);
        when(soaps.findById(OTHER_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> catalog.getSoap(OTHER_ID)).isInstanceOf(NotFoundException.class);
    }

    // ---- AdminUserService -------------------------------------------------------------------

    private static UserCreateRequest newAccount(Role role, String plate, String position, String password) {
        return new UserCreateRequest(role, "Carlo Mendoza", "carlo@example.com", "09190001234", password, plate,
                position);
    }

    @Test
    void onlyTheAdminManagesAccounts() {
        assertThatThrownBy(() -> accountsAdmin.create(STAFF_ID, newAccount(Role.CUSTOMER, null, null, null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessage("Only an admin can manage accounts.");
        assertThatThrownBy(() -> accountsAdmin.deactivate(STAFF_ID, OTHER_ID)).isInstanceOf(ForbiddenException.class);
        verify(users, never()).save(any());
    }

    @Test
    void emailsAreUnique() {
        when(users.existsByEmailIgnoreCase("carlo@example.com")).thenReturn(true);

        assertThatThrownBy(() -> accountsAdmin.create(ADMIN_ID, newAccount(Role.CUSTOMER, null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void riderAccountNeedsAPlate() {
        assertThatThrownBy(() -> accountsAdmin.create(ADMIN_ID, newAccount(Role.RIDER, null, null, PASSWORD)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Plate number must not be blank");
    }

    @Test
    void createdRiderHasAHashedPasswordAndStartsOffDuty() {
        UserResponse rider = accountsAdmin.create(ADMIN_ID, newAccount(Role.RIDER, "abc 1234", null, PASSWORD));

        assertThat(rider.role()).isEqualTo(Role.RIDER);
        assertThat(rider.plateNumber()).isEqualTo("ABC 1234");
        assertThat(rider.onDuty()).isFalse();
        assertThat(rider.orderCount()).isNull();
    }

    @Test
    void staffAccountKeepsItsMobileAndPosition() {
        UserResponse created = accountsAdmin.create(ADMIN_ID, newAccount(Role.STAFF, null, "Counter staff", null));

        assertThat(created.position()).isEqualTo("Counter staff");
        assertThat(created.mobileNumber()).isEqualTo("09190001234");
        assertThatThrownBy(() -> accountsAdmin.create(ADMIN_ID, newAccount(Role.STAFF, null, null, null)))
                .hasMessage("Position must not be blank");
    }

    @Test
    void adminCannotDeactivateThemselves() {
        assertThatThrownBy(() -> accountsAdmin.deactivate(ADMIN_ID, ADMIN_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("You cannot deactivate your own account.");
        when(users.findById(ADMIN_ID)).thenReturn(Optional.of(Fixtures.admin()));
        assertThatThrownBy(() -> accountsAdmin.update(ADMIN_ID, ADMIN_ID,
                new UserUpdateRequest("Admin Owner", null, AccountStatus.INACTIVE, null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deactivatingARiderTakesThemOffDuty() {
        Rider rider = Fixtures.rider("rider1@example.com", true);
        when(users.findById(OTHER_ID)).thenReturn(Optional.of(rider));

        accountsAdmin.deactivate(ADMIN_ID, OTHER_ID);

        assertThat(rider.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(rider.isOnDuty()).isFalse();
    }

    @Test
    void unknownAccountIsNotFound() {
        when(users.findById(OTHER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountsAdmin.update(ADMIN_ID, OTHER_ID,
                new UserUpdateRequest("Someone", null, AccountStatus.ACTIVE, null, null, null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Account not found");
    }
}
