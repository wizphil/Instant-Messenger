package com.wizphil.instantmessenger.dto;

import com.wizphil.instantmessenger.persistence.user.UserDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsDTO {
    private String id;
    private UserDetails userDetails;
}
