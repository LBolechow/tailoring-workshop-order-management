package pl.lukbol.dyplom.common;

public enum UserRole {

    ADMIN,
    EMPLOYEE,
    CLIENT;

    private static final String ROLE_PREFIX = "ROLE_";

    public String authority() {
        return ROLE_PREFIX + name();
    }
}

