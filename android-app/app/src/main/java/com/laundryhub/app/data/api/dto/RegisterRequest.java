package com.laundryhub.app.data.api.dto;

/** Body of {@code POST /api/auth/register} (00b "Create account"). */
public final class RegisterRequest {

    // OOP: Encapsulation — an immutable request.
    private final String fullName;
    private final String mobileNumber;
    private final String email;
    private final String password;
    private final String confirmPassword;
    private final boolean acceptedTerms;

    /**
     * @param fullName        Full name
     * @param mobileNumber    09XXXXXXXXX, no spaces
     * @param email           Email
     * @param password        Password
     * @param confirmPassword Confirm password
     * @param acceptedTerms   terms checkbox
     */
    public RegisterRequest(String fullName, String mobileNumber, String email, String password,
                           String confirmPassword, boolean acceptedTerms) {
        this.fullName = fullName;
        this.mobileNumber = mobileNumber;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.acceptedTerms = acceptedTerms;
    }

    /** @return the mobile number sent */
    public String getMobileNumber() {
        return mobileNumber;
    }
}
