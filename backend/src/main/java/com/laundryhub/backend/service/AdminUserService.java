package com.laundryhub.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.laundryhub.backend.domain.user.AccountStatus;
import com.laundryhub.backend.domain.user.Admin;
import com.laundryhub.backend.domain.user.ConsoleUser;
import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.domain.user.Role;
import com.laundryhub.backend.domain.user.Staff;
import com.laundryhub.backend.domain.user.User;
import com.laundryhub.backend.exception.ConflictException;
import com.laundryhub.backend.exception.ForbiddenException;
import com.laundryhub.backend.exception.NotFoundException;
import com.laundryhub.backend.repository.AdminRepository;
import com.laundryhub.backend.repository.CustomerRepository;
import com.laundryhub.backend.repository.OrderRepository;
import com.laundryhub.backend.repository.RiderRepository;
import com.laundryhub.backend.repository.StaffRepository;
import com.laundryhub.backend.repository.UserRepository;
import com.laundryhub.backend.web.dto.UserCreateRequest;
import com.laundryhub.backend.web.dto.UserResponse;
import com.laundryhub.backend.web.dto.UserUpdateRequest;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A4 Customers, riders &amp; staff. Accounts are never hard-deleted because orders and jobs refer
 * to them: "delete" deactivates the account so it can no longer sign in.
 */
@Service
public class AdminUserService {

    private final UserRepository users;
    private final CustomerRepository customers;
    private final RiderRepository riders;
    private final StaffRepository staff;
    private final AdminRepository admins;
    private final OrderRepository orders;
    private final PasswordEncoder passwordEncoder;
    private final AccountLookup accounts;

    /**
     * @param users           all accounts
     * @param customers       customers
     * @param riders          riders
     * @param staff           staff
     * @param admins          admins
     * @param orders          order counts per customer
     * @param passwordEncoder BCrypt
     * @param accounts        signed-in account lookup
     */
    public AdminUserService(UserRepository users, CustomerRepository customers, RiderRepository riders,
                            StaffRepository staff, AdminRepository admins, OrderRepository orders,
                            PasswordEncoder passwordEncoder, AccountLookup accounts) {
        this.users = users;
        this.customers = customers;
        this.riders = riders;
        this.staff = staff;
        this.admins = admins;
        this.orders = orders;
        this.passwordEncoder = passwordEncoder;
        this.accounts = accounts;
    }

    /**
     * A4 tabs: Customers ({@code CUSTOMER}), Riders ({@code RIDER}), Staff ({@code STAFF} and/or {@code ADMIN}).
     *
     * @param roles roles to list; empty = every role
     * @return accounts sorted by name
     */
    @Transactional(readOnly = true)
    public List<UserResponse> list(Set<Role> roles) {
        Set<Role> wanted = roles == null || roles.isEmpty() ? EnumSet.allOf(Role.class) : roles;
        List<User> found = new ArrayList<>();
        if (wanted.contains(Role.CUSTOMER)) {
            found.addAll(customers.findAllByOrderByFullNameAsc());
        }
        if (wanted.contains(Role.RIDER)) {
            found.addAll(riders.findAllByOrderByFullNameAsc());
        }
        if (wanted.contains(Role.ADMIN)) {
            found.addAll(admins.findAllByOrderByFullNameAsc());
        }
        if (wanted.contains(Role.STAFF)) {
            found.addAll(staff.findAllByOrderByFullNameAsc());
        }
        Map<Long, Long> orderCounts = orderCounts();
        return found.stream()
                .sorted(Comparator.comparing(User::getFullName, String.CASE_INSENSITIVE_ORDER))
                .map(user -> UserResponse.from(user, orderCounts.getOrDefault(user.getId(), 0L)))
                .toList();
    }

    /**
     * @param userId account id
     * @return the account
     */
    @Transactional(readOnly = true)
    public UserResponse get(Long userId) {
        User user = find(userId);
        return UserResponse.from(user, orderCounts().getOrDefault(userId, 0L));
    }

    /**
     * A4 "Add customer" / "Add rider" / "Add staff".
     *
     * @param editorId the signed-in admin
     * @param request  the new account
     * @return the account
     */
    @Transactional
    public UserResponse create(Long editorId, UserCreateRequest request) {
        requireAccountManager(editorId);
        if (users.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ConflictException("An account with this email already exists.");
        }
        User user = switch (request.role()) {
            case CUSTOMER -> new Customer(request.fullName(), request.email(), request.mobileNumber());
            case RIDER -> new Rider(request.fullName(), request.email(), request.mobileNumber(), request.plateNumber());
            case ADMIN -> new Admin(request.fullName(), request.email(), request.position());
            case STAFF -> new Staff(request.fullName(), request.email(), request.position());
        };
        if (user instanceof ConsoleUser) {
            user.changeMobileNumber(request.mobileNumber());
        }
        if (request.password() != null) {
            user.changePasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(users.save(user), 0);
    }

    /**
     * A4 "Edit".
     *
     * @param editorId the signed-in admin
     * @param userId   account id
     * @param request  new values
     * @return the account
     */
    @Transactional
    public UserResponse update(Long editorId, Long userId, UserUpdateRequest request) {
        requireAccountManager(editorId);
        User user = find(userId);
        user.rename(request.fullName());
        user.changeMobileNumber(request.mobileNumber());
        if (request.status() == AccountStatus.INACTIVE) {
            requireNotSelf(editorId, userId);
            user.deactivate();
        } else {
            user.activate();
        }
        if (user instanceof Rider rider && request.plateNumber() != null) {
            rider.changePlateNumber(request.plateNumber());
        }
        if (user instanceof ConsoleUser console && request.position() != null) {
            console.changePosition(request.position());
        }
        if (request.password() != null) {
            user.changePasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(user, orderCounts().getOrDefault(userId, 0L));
    }

    /**
     * Deactivates an account (soft delete).
     *
     * @param editorId the signed-in admin
     * @param userId   account id
     */
    @Transactional
    public void deactivate(Long editorId, Long userId) {
        requireAccountManager(editorId);
        requireNotSelf(editorId, userId);
        User user = find(userId);
        user.deactivate();
        if (user instanceof Rider rider) {
            rider.goOffDuty();
        }
    }

    // OOP: Polymorphism — canManageAccounts() is answered by Admin or Staff.
    private void requireAccountManager(Long editorId) {
        if (!accounts.consoleUser(editorId).canManageAccounts()) {
            throw new ForbiddenException("Only an admin can manage accounts.");
        }
    }

    private static void requireNotSelf(Long editorId, Long userId) {
        if (editorId.equals(userId)) {
            throw new ConflictException("You cannot deactivate your own account.");
        }
    }

    private User find(Long userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("Account not found"));
    }

    private Map<Long, Long> orderCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : orders.countPerCustomer()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return counts;
    }
}
