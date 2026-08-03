package pl.lukbol.dyplom.common;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Messages {

    // User-related messages
    public static final String ACCOUNT_CREATED = "Account created successfully.";
    public static final String PASSWORDS_DO_NOT_MATCH = "Passwords do not match.";
    public static final String PROFILE_UPDATED = "Profile updated successfully.";
    public static final String ACCOUNT_DELETED = "Account deleted successfully.";
    public static final String USER_NOT_FOUND_BY_EMAIL = "User not found with given email.";
    public static final String USER_NOT_FOUND_BY_ID = "User not found with given id";
    public static final String RESET_PASSWORD_LINK_SENT = "Password reset link sent to email!";
    public static final String EMAIL_ADDRES_ALREADY_EXIST = "User with same e-mail adress already exists!";
    public static final String USER_ADD_SUCCESS = "User added to system!";
    public static final String WELCOME_MESSAGE = "Welcome!";
    public static final String ROLE_NOT_FOUND = "Role not found: ";
    public static final String TOKEN_INVALID_OR_EXPIRED = "Token is invalid or has expired.";

    public static final String OAUTH_EMAIL_NOT_FOUND = "Email not found in OAuth2 response";

    public static final String USER_NOT_FOUND_MSG = "User not found!";

    // Conversation / Messaging
    public static final String CONVERSATION_NOT_FOUND = "Conversation not found.";
    public static final String LAST_MESSAGE_NOT_FOUND = "Last message not found.";
    public static final String ALERTS_REMOVED_MSG = "Notifications have been removed.";
    public static final String NOTIFICATION_CREATED_SUCCESS_MSG = "Notification created successfully.";
    public static final String PRATICIPANTS_LIST_IS_EMPTY = "Participants list in request is empty!";

    // Order-related messages
    public static final String NEW_ORDER_NOTIF = "There is a new order!";
    public static final String ORDER_UPDATED_NOTIF = "Order entry has been updated!";
    public static final String ORDER_NOT_FOUND = "Order not found";
    public static final String ORDER_DELETED = "Order deleted successfully.";

    // Price / Finance messages
    public static final String NEW_PRICE_ADDED_MESSAGE = "A new entry has been added!";
    public static final String PRICE_DELETE_MESSAGE = "A new entry has been deleted!";
    public static final String UPDATE_PRICE_MESSAGE = "Price entry has been updated!";

    public static final String PRICE_NOT_FOUND = "Price not found";

    // Material messages
    public static final String MATERIAL_NOT_FOUND = "Material not found";
    public static final String MATERIAL_UPDATED = "Material updated successfully";

}

