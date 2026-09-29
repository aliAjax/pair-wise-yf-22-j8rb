package com.generated.qualityTrace.repositories;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.generated.qualityTrace.models.User;

@Repository
public class UserRepository {
  private final JdbcTemplate jdbc;

  public UserRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public Optional<User> findByUsername(String username) {
    List<User> users = jdbc.query("""
        SELECT id, username, password_salt, password_hash, display_name, role, enabled, created_at
        FROM users WHERE username = ?
        """, new UserRowMapper(), username);
    return users.stream().findFirst();
  }

  private static final class UserRowMapper implements RowMapper<User> {
    @Override
    public User mapRow(ResultSet rs, int rowNum) throws SQLException {
      return new User(
          rs.getLong("id"),
          rs.getString("username"),
          rs.getString("password_salt"),
          rs.getString("password_hash"),
          rs.getString("display_name"),
          rs.getString("role"),
          rs.getBoolean("enabled"),
          rs.getObject("created_at", java.time.Instant.class)
      );
    }
  }
}
