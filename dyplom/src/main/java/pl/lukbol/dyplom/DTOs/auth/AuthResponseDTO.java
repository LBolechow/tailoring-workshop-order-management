package pl.lukbol.dyplom.DTOs.auth;

public record AuthResponseDTO(String token, String message) {
    @Override
    public String toString() {
        return "AuthResponseDTO[redacted]";
    }
}
