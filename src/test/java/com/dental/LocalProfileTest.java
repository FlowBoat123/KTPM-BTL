package com.dental;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = {"app.bootstrap.username=local-admin", "app.bootstrap.password=LocalPassword123!"})
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalProfileTest {
  static final Path DATABASE;

  static {
    try {
      DATABASE = Files.createTempDirectory("dental-local-test-").resolve("local.db");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void database(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on&busy_timeout=5000");
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;

  @Test
  void sqliteLocalProfileUsesConfiguredAdminAndDoesNotSeedDemoDoctors() throws Exception {
    mvc.perform(get("/api/doctors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        Map.of("username", "local-admin", "password", "LocalPassword123!"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user.role").value("ADMIN"));
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        Map.of("username", "admin", "password", "Admin123!"))))
        .andExpect(status().isUnauthorized());
  }
}
