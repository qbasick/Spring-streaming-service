package com.qbasic.authservice.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    String username;
    String password;
    String streamKey;
}
