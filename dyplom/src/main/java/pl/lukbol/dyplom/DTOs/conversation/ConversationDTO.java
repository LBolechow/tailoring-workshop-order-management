package pl.lukbol.dyplom.DTOs.conversation;

import pl.lukbol.dyplom.DTOs.user.UserDTO;

import java.util.List;

public record ConversationDTO(
        Long id,
        String name,
        boolean hidden,
        List<UserDTO> participants,
        UserDTO client
) {}
