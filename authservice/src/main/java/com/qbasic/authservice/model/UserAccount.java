package com.qbasic.authservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("user_accounts")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id @Column("id")
    Long id;

    @Column("username")
    String username;

    @Column("password")
    String password;

    @Column("stream_key")
    String streamKey;


}
