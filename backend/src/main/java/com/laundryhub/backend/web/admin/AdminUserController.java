package com.laundryhub.backend.web.admin;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.laundryhub.backend.domain.user.Role;
import com.laundryhub.backend.security.CurrentUserId;
import com.laundryhub.backend.service.AdminUserService;
import com.laundryhub.backend.web.dto.UserCreateRequest;
import com.laundryhub.backend.web.dto.UserResponse;
import com.laundryhub.backend.web.dto.UserUpdateRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A4 Customers, riders &amp; staff (role ADMIN only). */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin users", description = "A4 Customers, riders & staff (ADMIN)")
public class AdminUserController {

    private final AdminUserService users;

    /**
     * @param users account management
     */
    public AdminUserController(AdminUserService users) {
        this.users = users;
    }

    /**
     * @param role one or more roles (Customers tab: CUSTOMER; Riders: RIDER; Staff: STAFF and ADMIN)
     * @return accounts sorted by name
     */
    @GetMapping
    @Operation(summary = "A4 table (?role=CUSTOMER | RIDER | STAFF&role=ADMIN)")
    public List<UserResponse> list(@RequestParam(required = false) Set<Role> role) {
        return users.list(role);
    }

    /**
     * @param id account id
     * @return the account
     */
    @GetMapping("/{id}")
    @Operation(summary = "One account")
    public UserResponse get(@PathVariable Long id) {
        return users.get(id);
    }

    /**
     * @param editorId the signed-in admin
     * @param request  the new account
     * @return 201 with the account
     */
    @PostMapping
    @Operation(summary = "A4 Add customer / Add rider / Add staff")
    public ResponseEntity<UserResponse> create(@CurrentUserId Long editorId,
                                               @Valid @RequestBody UserCreateRequest request) {
        UserResponse user = users.create(editorId, request);
        return ResponseEntity.created(URI.create("/api/admin/users/" + user.id())).body(user);
    }

    /**
     * @param editorId the signed-in admin
     * @param id       account id
     * @param request  new values
     * @return the account
     */
    @PutMapping("/{id}")
    @Operation(summary = "A4 Edit")
    public UserResponse update(@CurrentUserId Long editorId, @PathVariable Long id,
                               @Valid @RequestBody UserUpdateRequest request) {
        return users.update(editorId, id, request);
    }

    /**
     * @param editorId the signed-in admin
     * @param id       account id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate an account (soft delete; orders keep their history)")
    public void delete(@CurrentUserId Long editorId, @PathVariable Long id) {
        users.deactivate(editorId, id);
    }
}
