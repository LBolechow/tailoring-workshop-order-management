package pl.lukbol.dyplom.exceptions;


public class ApplicationException {

    public static class UserWithEmailAlreadyExistsException extends RuntimeException {
        public UserWithEmailAlreadyExistsException(String message) {
            super(message);
        }
    }

    public static class PasswordsMismatchException extends RuntimeException {
        public PasswordsMismatchException(String message) {
            super(message);
        }
    }


    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class ParticipantsListIsEmptyException extends RuntimeException {
        public ParticipantsListIsEmptyException(String message) {
            super(message);
        }
    }
    public static class ConversationNotFoundException extends RuntimeException {
        public ConversationNotFoundException(String message) {
            super(message);
        }
    }
    public static class LastMessageNotFoundException extends RuntimeException {
        public LastMessageNotFoundException(String message) {
            super(message);
        }
    }
    public static class InvalidDateException extends RuntimeException {
        public InvalidDateException(String message) {
            super(message);
        }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException(String message) {
            super(message);
        }
    }
    public static class MaterialNotFoundException extends RuntimeException {
        public MaterialNotFoundException(String message) {
            super(message);
        }
    }
    public static class PriceNotFoundException extends RuntimeException {
        public PriceNotFoundException(String message) {
            super(message);
        }
    }


}
