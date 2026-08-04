package pl.lukbol.dyplom.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Getter
@NoArgsConstructor
public class SecurityPaths {
    public static final List<String> skipFilterUrls = List.of(
            "/api/auth/login",
            "/register",
            "/send-new-password",
            "/prices",
            "/order/checkOrder/**",
            "/error",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**"
    );
    public final String[] CLIENT_EMPLOYEE_ADMIN_PATHS = {
            "/user",
            "/profile/**",
            "/currentDate",
            "/index",
            "/removeAlerts",
            "/create-notification",
            "/ws-chat/**",
            "/api/conversation",
            "/conversation/**",
            "/{conversationId}/latest-message",
            "/markConversationAsRead/**",
            "/checkIfConversationRead/**",
            "/topic/**"
    };
    public final String[] ADMIN_PATHS = {
            "/add",
            "/search-users",
            "/users/findByRole",
            "/users/{id}",
            "/users/update/**",
            "/add-price",
            "/delete-price/**",
            "/update-price/**"
    };
    public final String[] ADMIN_EMPLOYEE_PATHS = {
            "/order/add",
            "/order/getDailyOrders",
            "/order/getOrderDetails/**",
            "/order/edit/**",
            "/order/delete/**",
            "/order/checkAvailability",
            "/order/checkAvailabilityNextDay/**",
            "/order/otherEmployee/**",
            "/order/search",
            "/material/**",
            "/users/employees-and-admins",
            "/user/employees-and-admins",
            "/api/createConversation",
            "/api/employee/conversations",
            "/get_conversations",
            "/getConversationParticipants/**",
            "/clearSeenByUserIds/**",
            "/checkSeen/**",
            "/hide/**"
    };

    public final String[] PERMIT_ALL_PATHS = {
            "/api/auth/login",
            "/register",
            "/send-new-password",
            "/prices",
            "/order/checkOrder/**",
            "/error",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/h2-console/**"
    };
}