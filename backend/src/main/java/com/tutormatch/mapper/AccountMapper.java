package com.tutormatch.mapper;

import static com.tutormatch.mapper.JsonFields.*;

import com.tutormatch.dto.auth.AccountProfile;
import com.tutormatch.model.Account;
import com.tutormatch.model.UserCredentials;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AccountMapper {
  public Account account(Map<String, Object> row) {
    return new Account(
        text(row, "id"), text(row, "email"), text(row, "full_name"), text(row, "role"));
  }

  public AccountProfile profile(Map<String, Object> row) {
    return new AccountProfile(
        text(row, "id"),
        text(row, "email"),
        text(row, "full_name"),
        text(row, "role"),
        text(row, "phone"));
  }

  public UserCredentials credentials(Map<String, Object> row) {
    return new UserCredentials(
        account(row), text(row, "password_hash"), text(row, "phone"), bool(row, "email_verified"));
  }
}
