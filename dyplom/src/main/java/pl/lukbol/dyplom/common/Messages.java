package pl.lukbol.dyplom.common;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class Messages {

    // User-related messages
    public static final String ACCOUNT_CREATED = "Konto zostało utworzone pomyślnie.";
    public static final String PASSWORDS_DO_NOT_MATCH = "Hasła nie są zgodne.";
    public static final String PROFILE_UPDATED = "Profil został zaktualizowany pomyślnie.";
    public static final String ACCOUNT_DELETED = "Konto zostało usunięte pomyślnie.";
    public static final String USER_NOT_FOUND_BY_EMAIL = "Nie znaleziono użytkownika z podanym adresem email.";
    public static final String USER_NOT_FOUND_BY_ID = "Nie znaleziono użytkownika z podanym id.";
    public static final String RESET_PASSWORD_LINK_SENT = "Nowe hasło zostało wysłane na adres email!";
    public static final String EMAIL_ADDRES_ALREADY_EXIST = "Użytkownik z takim adresem email już istnieje!";
    public static final String USER_ADD_SUCCESS = "Użytkownik został dodany do systemu!";
    public static final String WELCOME_MESSAGE = "Witamy!";
    public static final String ROLE_NOT_FOUND = "Role not found: ";
    public static final String TOKEN_INVALID_OR_EXPIRED = "Token is invalid or has expired.";
    public static final String OAUTH_EMAIL_NOT_FOUND = "Email not found in OAuth2 response.";

    // Conversation / Messaging
    public static final String CONVERSATION_NOT_FOUND = "Conversation not found.";
    public static final String LAST_MESSAGE_NOT_FOUND = "Last message not found.";
    public static final String ALERTS_REMOVED_MSG = "Powiadomienia zostały usunięte.";
    public static final String NOTIFICATION_CREATED_SUCCESS_MSG = "Powiadomienie zostało utworzone pomyślnie.";
    public static final String PARTICIPANTS_LIST_IS_EMPTY = "Participants list in request is empty.";
    public static final String CONVERSATION_CREATED = "Konwersacja została utworzona pomyślnie.";
    public static final String CONVERSATION_MARKED_AS_READ = "Wiadomości zostały oznaczone jako przeczytane.";
    public static final String CONVERSATION_SEEN_CLEARED = "Lista przeczytanych została wyczyszczona.";
    public static final String CONVERSATION_RESTORED = "Konwersacja została przywrócona.";
    public static final String CONVERSATION_HIDDEN = "Konwersacja została ukryta.";

    // Order-related messages
    public static final String NEW_ORDER_NOTIF = "Pojawiło się nowe zlecenie!";
    public static final String ORDER_UPDATED_NOTIF = "Zlecenie zostało zaktualizowane!";
    public static final String ORDER_NOT_FOUND = "Order not found.";
    public static final String ORDER_DELETED = "Zlecenie zostało usunięte pomyślnie.";

    // Price / Finance messages
    public static final String NEW_PRICE_ADDED_MESSAGE = "Nowy wpis cennika został dodany!";
    public static final String PRICE_DELETE_MESSAGE = "Wpis cennika został usunięty!";
    public static final String UPDATE_PRICE_MESSAGE = "Wpis cennika został zaktualizowany!";
    public static final String PRICE_NOT_FOUND = "Price not found.";

    // Material messages
    public static final String MATERIAL_NOT_FOUND = "Material not found.";
    public static final String MATERIAL_UPDATED = "Material updated successfully.";

    // Login
    public static final String LOGIN_SUCCESS = "Login successful.";
    public static final String LOGOUT_SUCCESS = "Logout successful.";


    // E-mail
    public static final String RESET_PASSWORD_EMAIL_SUBJECT = "Twoje nowe hasło!";
    public static final String RESET_PASSWORD_EMAIL_BODY = "Twoje nowe hasło to: ";

    // OrderUtils
    public static final String SLOT_AVAILABLE = "Termin dostępny.";
    public static final String NO_AVAILABLE_EMPLOYEES = "Brak dostępnych pracowników w ramach dni roboczych.";
    public static final String INVALID_DATE_FORMAT = "Invalid date format: ";
    public static final String USER_NOT_FOUND_BY_NAME = "User not found by name: ";
    public static final String NULL_DATE_EXCEPTION = "taskStartDateTime and taskEndDateTime cannot be null.";
    public static final String ORDER_STATUS_IN_PROGRESS = "W trakcie";
}

