package pl.lukbol.dyplom.common;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public final class Messages {

    // Validation messages for inbound DTOs (annotation constants)
    public static final String VALIDATION_EMAIL_REQUIRED = "Adres email jest wymagany.";
    public static final String VALIDATION_EMAIL_INVALID = "Nieprawidłowy format adresu email.";
    public static final String VALIDATION_EMAIL_TOO_LONG = "Adres email nie może przekraczać 254 znaków.";
    public static final String VALIDATION_NAME_REQUIRED = "Imię i nazwisko jest wymagane.";
    public static final String VALIDATION_NAME_TOO_LONG = "Imię i nazwisko nie może przekraczać 255 znaków.";
    public static final String VALIDATION_PASSWORD_REQUIRED = "Hasło jest wymagane.";
    public static final String VALIDATION_PASSWORD_MIN_8 = "Hasło musi mieć co najmniej 8 znaków.";
    public static final String VALIDATION_PASSWORD_MIN_12 = "Nowe hasło musi mieć co najmniej 12 znaków.";
    public static final String VALIDATION_PASSWORD_TOO_LONG = "Hasło nie może przekraczać 128 znaków.";
    public static final String VALIDATION_REPEAT_PASSWORD_REQUIRED = "Powtórzenie hasła jest wymagane.";
    public static final String VALIDATION_ROLE_REQUIRED = "Rola jest wymagana.";
    public static final String VALIDATION_ROLE_INVALID = "Nieprawidłowa rola użytkownika.";
    public static final String VALIDATION_ORDER_DESCRIPTION_TOO_LONG = "Opis zamówienia nie może przekraczać 255 znaków.";
    public static final String VALIDATION_ORDER_CLIENT_TOO_LONG = "Nazwa klienta nie może przekraczać 255 znaków.";
    public static final String VALIDATION_ORDER_PHONE_TOO_LONG = "Numer telefonu nie może przekraczać 32 znaków.";
    public static final String VALIDATION_ORDER_EMPLOYEE_TOO_LONG = "Nazwa pracownika nie może przekraczać 255 znaków.";
    public static final String VALIDATION_ORDER_DATE_TOO_LONG = "Wartość daty jest zbyt długa.";
    public static final String VALIDATION_ORDER_STATUS_INVALID = "Dozwolone statusy: W trakcie, Zakończone, Anulowane.";
    public static final String VALIDATION_ORDER_ITEMS_TOO_MANY = "Jedno zlecenie może zawierać maksymalnie 100 materiałów.";
    public static final String VALIDATION_ORDER_ITEM_REQUIRED = "Nazwa materiału nie może być pusta.";
    public static final String VALIDATION_ORDER_ITEM_TOO_LONG = "Nazwa materiału nie może przekraczać 255 znaków.";
    public static final String VALIDATION_PRICE_ITEM_REQUIRED = "Nazwa pozycji cennika jest wymagana.";
    public static final String VALIDATION_PRICE_ITEM_TOO_LONG = "Nazwa pozycji cennika nie może przekraczać 255 znaków.";
    public static final String VALIDATION_PRICE_VALUE_REQUIRED = "Cena jest wymagana.";
    public static final String VALIDATION_PRICE_VALUE_TOO_LONG = "Wartość ceny nie może przekraczać 255 znaków.";
    public static final String VALIDATION_NOTIFICATION_MESSAGE_REQUIRED = "Treść powiadomienia jest wymagana.";
    public static final String VALIDATION_NOTIFICATION_MESSAGE_TOO_LONG = "Treść powiadomienia nie może przekraczać 255 znaków.";
    public static final String VALIDATION_NOTIFICATION_RECIPIENTS_REQUIRED = "Wskaż co najmniej jednego odbiorcę powiadomienia.";
    public static final String VALIDATION_NOTIFICATION_RECIPIENT_INVALID = "Identyfikator odbiorcy musi być dodatni.";
    public static final String VALIDATION_NOTIFICATION_RECIPIENTS_TOO_MANY = "Można wskazać maksymalnie 100 odbiorców.";
    public static final String VALIDATION_OAUTH_CODE_REQUIRED = "Kod autoryzacyjny jest wymagany.";
    public static final String VALIDATION_OAUTH_CODE_TOO_LONG = "Kod autoryzacyjny jest zbyt długi.";
    public static final String VALIDATION_RESET_TOKEN_REQUIRED = "Token resetowania hasła jest wymagany.";
    public static final String VALIDATION_RESET_TOKEN_TOO_LONG = "Token resetowania hasła jest zbyt długi.";
    public static final String VALIDATION_MATERIAL_CHECKED_REQUIRED = "Pole checked jest wymagane.";

    // User-related messages
    public static final String ACCOUNT_CREATED = "Konto zostało utworzone pomyślnie.";
    public static final String PASSWORDS_DO_NOT_MATCH = "Hasła nie są zgodne.";
    public static final String PROFILE_UPDATED = "Profil został zaktualizowany pomyślnie.";
    public static final String ACCOUNT_DELETED = "Konto zostało usunięte pomyślnie.";
    public static final String USER_NOT_FOUND_BY_EMAIL = "Nie znaleziono użytkownika z podanym adresem email.";
    public static final String USER_NOT_FOUND_BY_ID = "Nie znaleziono użytkownika z podanym id.";
    public static final String RESET_PASSWORD_LINK_SENT = "Link do resetowania hasła został wysłany na adres email.";
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

    // Order validation (compile-time constants also usable in Bean Validation annotations)
    public static final String ORDER_DESCRIPTION_REQUIRED = "Opis zlecenia jest wymagany.";
    public static final String ORDER_CLIENT_REQUIRED = "Nazwa klienta jest wymagana.";
    public static final String ORDER_EMAIL_REQUIRED = "Adres email klienta jest wymagany.";
    public static final String ORDER_EMAIL_INVALID = "Nieprawidłowy format adresu email.";
    public static final String ORDER_PHONE_REQUIRED = "Numer telefonu jest wymagany.";
    public static final String ORDER_DURATION_MIN = "Czas realizacji musi wynosić co najmniej 0.5 godziny.";
    public static final String ORDER_DURATION_MAX = "Czas realizacji nie może przekraczać 8 godzin.";
    public static final String ORDER_START_REQUIRED = "Data rozpoczęcia jest wymagana.";
    public static final String ORDER_END_REQUIRED = "Data zakończenia jest wymagana.";
    public static final String ORDER_EMPLOYEE_REQUIRED = "Pracownik jest wymagany.";
    public static final String ORDER_PRICE_NONNEGATIVE = "Cena nie może być ujemna.";
    public static final String ORDER_STATUS_REQUIRED = "Status zlecenia jest wymagany.";
    public static final String ORDER_ITEMS_REQUIRED = "Lista materiałów jest wymagana.";

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
    public static final String RESET_PASSWORD_EMAIL_SUBJECT = "Resetowanie hasła";
    public static final String RESET_PASSWORD_EMAIL_BODY = "Twoje nowe hasło to: ";

    // OrderUtils
    public static final String SLOT_AVAILABLE = "Termin dostępny.";
    public static final String NO_AVAILABLE_EMPLOYEES = "Brak dostępnych pracowników w ramach dni roboczych.";
    public static final String INVALID_DATE_FORMAT = "Invalid date format: ";
    public static final String USER_NOT_FOUND_BY_NAME = "User not found by name: ";
    public static final String NULL_DATE_EXCEPTION = "taskStartDateTime and taskEndDateTime cannot be null.";
    public static final String ORDER_STATUS_IN_PROGRESS = "W trakcie";
    public static final String CANNOT_DELETE_EMPLOYEE_WITH_ORDERS = "Nie można usunąć pracownika, który ma przypisane zamówienia.";
    public static final String RESET_MAIL_GENERIC = "If the account exists, a reset email has been sent.";
    public static final String RESET_PASSWORD_SUCCESS = "Password has been reset successfully.";
    public static final String CONVERSATION_ACCESS_DENIED = "Access denied";
    public static final String ORDER_STATUS_COMPLETED = "Zakończone";
    public static final String ORDER_STATUS_CANCELLED = "Anulowane";
    public static final String INVALID_ORDER_STATUS = "Nieprawidłowy status zlecenia.";
    public static final String AMBIGUOUS_EMPLOYEE = "Niejednoznaczna nazwa pracownika: ";
    public static final String EMPLOYEE_NOT_FOUND = "Nie znaleziono pracownika (id: ";
    public static final String ORDER_SLOT_CONFLICT = "Pracownik ma już zlecenie w wybranym terminie.";
    public static final String INVALID_ORDER_RANGE = "Koniec zlecenia musi być późniejszy niż początek.";
    public static final String INVALID_ORDER_DATE = "Nieprawidłowa data zlecenia. Oczekiwano ISO yyyy-MM-dd'T'HH:mm:ss.";
    public static final String EMPTY_ORDER_DATE = "Data zlecenia jest wymagana.";
    public static final String OUTSIDE_WORKING_HOURS = "Zlecenie musi mieścić się w godzinach pracy 08:00–16:00, od poniedziałku do piątku.";
    public static final String INVALID_BOOKING_DURATION = "Czas realizacji musi odpowiadać przedziałowi dat i wynosić od 30 minut do 8 godzin.";
    public static final String INVALID_BOOKING_START = "Początek zlecenia musi przypadać na pełną lub połowę godziny.";
    public static final String ORDER_ACCESS_DENIED = "Brak uprawnień do tego zlecenia.";
    public static final String INVALID_AUTHENTICATION = "Brak poprawnego uwierzytelnienia.";
    public static final String INTERNAL_SERVER_ERROR = "An unexpected server error occurred";
    public static final String INVALID_OAUTH2_CODE = "Invalid or expired authorization code";
    public static final String INVALID_RESET_TOKEN = "Invalid or expired password reset token";
    public static final String INVALID_DURATION = "Duration must be greater than zero";
    public static final String MISSING_DATE = "Start and end dates are required.";
    public static final String SYSTEM_NOTIFICATION_CREATOR = "System";
}

