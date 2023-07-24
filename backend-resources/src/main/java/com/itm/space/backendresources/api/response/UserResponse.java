package com.itm.space.backendresources.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(force = true)
@AllArgsConstructor
@Data
public class UserResponse {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final List<String> roles;
    private final List<String> groups;
}
