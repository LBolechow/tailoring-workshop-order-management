package pl.lukbol.dyplom.unitTests;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import pl.lukbol.dyplom.DTOs.auth.LoginRequestDTO;
import pl.lukbol.dyplom.DTOs.notification.NotificationRequestDTO;
import pl.lukbol.dyplom.DTOs.order.AddOrderDTO;
import pl.lukbol.dyplom.DTOs.order.EditOrderDTO;
import pl.lukbol.dyplom.DTOs.order.UpdateMaterialCheckedDTO;
import pl.lukbol.dyplom.DTOs.price.PriceRequestDTO;
import pl.lukbol.dyplom.DTOs.user.*;
import pl.lukbol.dyplom.common.Messages;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InputDtoValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void validOrderRequestPasses() {
        assertThat(validator.validate(order("W trakcie", List.of("Zamek")))).isEmpty();
    }

    @Test
    void orderRejectsInvalidStatus() {
        var errors = validator.validate(order("Nieznany", List.of("Zamek")));
        assertThat(errors).anySatisfy(e -> assertThat(e.getMessage()).isEqualTo(Messages.VALIDATION_ORDER_STATUS_INVALID));
    }

    @Test
    void orderRejectsBlankMaterialItemButAllowsEmptyMaterials() {
        assertThat(validator.validate(order("W trakcie", List.of(" ")))).isNotEmpty();
        assertThat(validator.validate(order("W trakcie", List.of()))).isEmpty();
    }

    @Test
    void orderRejectsTooLongDescription() {
        var source = order("W trakcie", List.of());
        var invalid = new AddOrderDTO("x".repeat(256), source.clientName(), source.email(), source.phoneNumber(),
                source.hours(), source.startDate(), source.endDate(), source.selectedUser(), source.price(),
                source.status(), source.items());
        assertThat(validator.validate(invalid)).isNotEmpty();
    }

    @Test
    void editingOrderValidatesMaterialsToo() {
        var request = order("W trakcie", List.of(" "));
        var edited = new EditOrderDTO(request.description(), request.clientName(), request.email(),
                request.phoneNumber(), request.hours(), request.startDate(), request.endDate(),
                request.selectedUser(), request.price(), request.status(), request.items());
        assertThat(validator.validate(edited)).isNotEmpty();
    }

    @Test
    void notificationMustHaveTextAndValidRecipients() {
        assertThat(validator.validate(new NotificationRequestDTO("", List.of()))).hasSizeGreaterThanOrEqualTo(2);
        assertThat(validator.validate(new NotificationRequestDTO("OK", List.of(-1L)))).isNotEmpty();
        assertThat(validator.validate(new NotificationRequestDTO("OK", java.util.Collections.singletonList(null)))).isNotEmpty();
        assertThat(validator.validate(new NotificationRequestDTO("OK", List.of(1L)))).isEmpty();
    }

    @Test
    void priceCatalogCannotAcceptBlankValues() {
        assertThat(validator.validate(new PriceRequestDTO(" ", "50"))).isNotEmpty();
        assertThat(validator.validate(new PriceRequestDTO("Usluga", " "))).isNotEmpty();
        assertThat(validator.validate(new PriceRequestDTO("Usluga", "50 zl"))).isEmpty();
    }

    @Test
    void userRoleAndEmailValidated() {
        assertThat(validator.validate(new AddUserDTO("Jan", "zly-email", "secret123", "ROLE_UNKNOWN"))).hasSizeGreaterThanOrEqualTo(2);
        assertThat(validator.validate(new AddUserDTO("Jan", "jan@test.pl", "secret123", "ROLE_EMPLOYEE"))).isEmpty();
        assertThat(validator.validate(new UpdateUserRequest("Jan", "jan@test.pl", "ROLE_X"))).isNotEmpty();
    }

    @Test
    void registerAndResetPasswordsHaveDifferentMinimumLengths() {
        assertThat(validator.validate(new RegisterRequestDTO("Jan", "jan@test.pl", "short"))).isNotEmpty();
        assertThat(validator.validate(new ResetPasswordRequestDTO("token", "secret123"))).isNotEmpty();
        assertThat(validator.validate(new ResetPasswordRequestDTO("token", "strongPassword123"))).isEmpty();
    }

    @Test
    void updateProfileRequiresAllThreeFields() {
        assertThat(validator.validate(new UpdateProfileRequestDTO("Jan", null, null))).isNotEmpty();
        assertThat(validator.validate(new UpdateProfileRequestDTO("Jan", "strong123", "strong123"))).isEmpty();
    }

    @Test
    void materialPatchRequiresExplicitCheckedValue() {
        assertThat(validator.validate(new UpdateMaterialCheckedDTO(null))).isNotEmpty();
        assertThat(validator.validate(new UpdateMaterialCheckedDTO(false))).isEmpty();
    }

    @Test
    void secretBearingRecordsRedactTheirToStringOutput() {
        assertThat(new LoginRequestDTO("a@b.pl", "password123").toString()).doesNotContain("password123");
        assertThat(new RegisterRequestDTO("Jan", "a@b.pl", "password123").toString()).doesNotContain("password123");
        assertThat(new ResetPasswordRequestDTO("secretToken", "newPassword123").toString())
                .doesNotContain("secretToken").doesNotContain("newPassword123");
        assertThat(new UpdateProfileRequestDTO("Jan", "newPassword123", "newPassword123").toString())
                .doesNotContain("newPassword123");
        assertThat(new pl.lukbol.dyplom.DTOs.auth.AuthResponseDTO("jwt-token", "OK").toString())
                .doesNotContain("jwt-token");
    }

    @Test
    void loginRequiresCredentialsButDoesNotEnforceRegistrationPasswordLength() {
        assertThat(validator.validate(new LoginRequestDTO("bad", " "))).isNotEmpty();
        assertThat(validator.validate(new LoginRequestDTO("test@example.com", "legacy"))).isEmpty();
    }

    @Test
    void resetTokenAndEmailCannotBeBlank() {
        assertThat(validator.validate(new ForgotPasswordRequestDTO(""))).isNotEmpty();
        assertThat(validator.validate(new ResetPasswordRequestDTO(" ", "strongPassword123"))).isNotEmpty();
    }

    private static AddOrderDTO order(String status, List<String> items) {
        return new AddOrderDTO("Poprawki", "Jan Kowalski", "jan@example.com", "+48 555 555 555",
                1.0, "2026-11-02T09:00:00", "2026-11-02T10:00:00", "Pracownik", 100,
                status, items);
    }
}
