package com.qbasic.authservice.dto;


import lombok.Data;

@Data
public class UsernameAndStreamKeyRequest {
    String username;
    String streamKey;
}
