package com.dental;

import com.dental.entity.Account;
import com.dental.entity.LoginSession;
import com.dental.repository.AccountRepository;
import com.dental.repository.LoginSessionRepository;
import com.dental.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import(ApiIntegrationTest.FixedTime.class)
class ApiIntegrationTest {
  static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
  static final Path DATABASE;

  static {
    try {
      DATABASE = Files.createTempDirectory("dental-tests-").resolve("test.db");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry r) {
    r.add(
        "spring.datasource.url",
        () -> "jdbc:sqlite:" + DATABASE + "?foreign_keys=on&busy_timeout=5000");
    r.add("app.jwt-secret", () -> "test-key-only-at-least-32-characters-long");
  }

  @TestConfiguration
  static class FixedTime {
    @Bean
    @Primary
    Clock testClock() {
      return Clock.fixed(NOW, ZoneId.of("Asia/Ho_Chi_Minh"));
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired AccountRepository accounts;
  @Autowired LoginSessionRepository sessions;
  @Autowired JwtService jwt;

  @BeforeEach
  void reset() {
    jdbc.update("DELETE FROM login_sessions");
    jdbc.update("DELETE FROM appointments");
    jdbc.update("DELETE FROM doctors WHERE id > 2");
    jdbc.update("DELETE FROM accounts WHERE id > 3");
    jdbc.update("UPDATE accounts SET active=1");
  }

  String login(String username, String password) throws Exception {
    return json(mvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        mapper.writeValueAsString(
                            Map.of("username", username, "password", password))))
            .andExpect(status().isOk())
            .andReturn())
        .get("accessToken")
        .asText();
  }

  String admin() throws Exception {
    return login("admin", "Admin123!");
  }

  String doctor1() throws Exception {
    return login("doctor1", "Doctor123!");
  }

  JsonNode json(MvcResult r) throws Exception {
    return mapper.readTree(r.getResponse().getContentAsString());
  }

  Map<String, Object> booking(int service, String time) {
    return new LinkedHashMap<>(
        Map.of(
            "patientName",
            "Test Patient",
            "phone",
            "0912345678",
            "email",
            "test@example.com",
            "serviceId",
            service,
            "preferredDate",
            "2030-01-02",
            "preferredTime",
            time,
            "note",
            "Test note"));
  }

  JsonNode book(int service, String time) throws Exception {
    return json(
        mvc.perform(
                post("/api/appointments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(booking(service, time))))
            .andExpect(status().isCreated())
            .andReturn());
  }

  ResultActions assign(JsonNode appointment, int doctor, String time, String token)
      throws Exception {
    return mvc.perform(
        patch("/api/admin/appointments/" + appointment.get("id").asLong() + "/assign")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                mapper.writeValueAsString(
                    Map.of(
                        "doctorId", doctor, "appointmentDate", "2030-01-02", "startTime", time))));
  }

  ResultActions change(JsonNode a, String role, String next, String token) throws Exception {
    return mvc.perform(
        patch("/api/" + role + "/appointments/" + a.get("id").asLong() + "/status")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(Map.of("status", next))));
  }

  @Test
  void publicCatalogAndPendingBooking() throws Exception {
    mvc.perform(get("/api/clinic"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openingTime").value("08:00:00"));
    mvc.perform(get("/api/services"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
    mvc.perform(get("/api/doctors"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].passwordHash").doesNotExist());
    mvc.perform(get("/api/doctors/1")).andExpect(status().isOk());
    JsonNode a = book(1, "09:00");
    assertEquals("PENDING", a.get("status").asText());
    assertTrue(a.get("doctorId").isNull());
    assertDoesNotThrow(() -> UUID.fromString(a.get("code").asText()));
    mvc.perform(get("/api/appointments/" + a.get("code").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(a.get("id").asLong()));
    assertTrue(Files.size(DATABASE) > 0);
  }

  @Test
  void completeBookingAssignmentConfirmationAndCompletion() throws Exception {
    JsonNode a = book(2, "09:00");
    String admin = admin(), doctor = doctor1();
    assign(a, 1, "09:00", admin)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ASSIGNED"))
        .andExpect(jsonPath("$.endTime").value("10:00:00"));
    mvc.perform(get("/api/doctor/appointments").header("Authorization", "Bearer " + doctor))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
    change(a, "doctor", "CONFIRMED", doctor).andExpect(status().isOk());
    change(a, "doctor", "COMPLETED", doctor).andExpect(status().isOk());
    change(a, "doctor", "ASSIGNED", doctor).andExpect(status().isConflict());
    assign(a, 2, "11:00", admin).andExpect(status().isConflict());
  }

  @Test
  void doctorCannotAccessAnotherDoctorsAppointmentOrAdminRoutes() throws Exception {
    JsonNode a = book(1, "09:00");
    assign(a, 1, "09:00", admin()).andExpect(status().isOk());
    String other = login("doctor2", "Doctor123!");
    mvc.perform(
            get("/api/doctor/appointments")
                .header("Authorization", "Bearer " + other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(
            get("/api/doctor/appointments/" + a.get("id").asLong())
                .header("Authorization", "Bearer " + other))
        .andExpect(status().isForbidden());
    change(a, "doctor", "COMPLETED", other).andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/appointments").header("Authorization", "Bearer " + other))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/admin/appointments")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/doctor/profile").header("Authorization", "Bearer " + admin()))
        .andExpect(status().isForbidden());
  }

  @Test
  void doctorCanCancelAssignedAppointmentButCannotReopenIt() throws Exception {
    JsonNode a = book(1, "09:00");

    assign(a, 1, "09:00", admin())
            .andExpect(status().isOk());

    String doctor = doctor1();

    change(a, "doctor", "CANCELLED", doctor)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));

    change(a, "doctor", "CONFIRMED", doctor)
            .andExpect(status().isConflict());
  }

  @Test
  void overlappingDurationIsRejectedButAdjacentAndOtherDoctorAreAllowed() throws Exception {
    String token = admin();
    JsonNode a = book(2, "09:00"), b = book(1, "09:30"), c = book(1, "10:00");
    assign(a, 1, "09:00", token).andExpect(status().isOk());
    assign(b, 1, "09:30", token).andExpect(status().isConflict());
    assign(b, 2, "09:30", token).andExpect(status().isOk());
    assign(c, 1, "10:00", token).andExpect(status().isOk());
  }

  @Test
  void availableSlotsRespectServiceDurationAndCancellationReleasesSlot() throws Exception {
    String token = admin();
    JsonNode a = book(2, "09:00");
    assign(a, 1, "09:00", token).andExpect(status().isOk());
    JsonNode slots =
        json(
            mvc.perform(get("/api/doctors/1/available-slots?date=2030-01-02&serviceId=2"))
                .andExpect(status().isOk())
                .andReturn());
    String values = slots.get("slots").toString();
    assertFalse(values.contains("08:30"));
    assertFalse(values.contains("09:00"));
    assertFalse(values.contains("09:30"));
    assertTrue(values.contains("10:00"));
    mvc.perform(delete("/api/appointments/" + a.get("code").asText()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    JsonNode other = book(2, "09:00");
    assign(other, 1, "09:00", token).andExpect(status().isOk());
    assign(a, 1, "10:00", token).andExpect(status().isConflict());
  }

  @Test
  void reassignmentReleasesPreviousDoctorAndRequiresConfirmationAgain() throws Exception {
    String token = admin();
    JsonNode a = book(2, "09:00");
    assign(a, 1, "09:00", token).andExpect(status().isOk());
    change(a, "doctor", "CONFIRMED", doctor1()).andExpect(status().isOk());
    assign(a, 2, "10:00", token)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ASSIGNED"));
    assign(book(2, "09:00"), 1, "09:00", token).andExpect(status().isOk());
  }

  @Test
  void invalidInputPastTimeGridAndClosingHoursAreRejected() throws Exception {
    for (String time : List.of("07:30", "17:00", "09:10", "09:00:01")) {
      mvc.perform(
              post("/api/appointments")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(mapper.writeValueAsString(booking(1, time))))
          .andExpect(status().isBadRequest());
    }
    var r = booking(1, "09:00");
    r.put("preferredDate", "2020-01-01");
    mvc.perform(
            post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(r)))
        .andExpect(status().isBadRequest());
    r = booking(1, "09:00");
    r.put("phone", "abc");
    mvc.perform(
            post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(r)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.phone").exists());
    mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(booking(999, "09:00"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void customerCannotChooseDoctorAndInvalidEnumsAreRejected() throws Exception {
    var r = booking(1, "09:00");
    r.put("doctorId", 1);
    mvc.perform(
            post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(r)))
        .andExpect(status().isBadRequest());
    change(book(1, "09:00"), "admin", "UNKNOWN", admin()).andExpect(status().isBadRequest());
  }

  @Test
  void pendingCannotBeCompletedOrAssignedByStatusPatch() throws Exception {
    JsonNode a = book(1, "09:00");
    String token = admin();
    change(a, "admin", "COMPLETED", token).andExpect(status().isConflict());
    change(a, "admin", "ASSIGNED", token).andExpect(status().isConflict());
    change(a, "admin", "CANCELLED", token).andExpect(status().isOk());
    change(a, "admin", "PENDING", token).andExpect(status().isConflict());
  }

  @Test
  void authenticationLogoutAndTokenTampering() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(Map.of("username", "admin", "password", "wrong"))))
        .andExpect(status().isUnauthorized());
    String token = admin();
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer invalid"))
        .andExpect(status().isUnauthorized());
    String tampered = (token.charAt(0) == 'a' ? "b" : "a") + token.substring(1);
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tampered))
        .andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void expiredAndUnregisteredTokensAreRejected() throws Exception {
    Account account = accounts.findByUsername("admin").orElseThrow();
    String id = UUID.randomUUID().toString();
    Instant issued = NOW.minusSeconds(1000), expiry = NOW.minusSeconds(500);
    sessions.save(new LoginSession(id, account, expiry));
    String expired = jwt.issue(account, id, issued, expiry);
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expired))
        .andExpect(status().isUnauthorized());
    String unregistered =
        jwt.issue(account, UUID.randomUUID().toString(), NOW, NOW.plusSeconds(500));
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + unregistered))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void createUpdateAndDisableDoctorRevokesExistingToken() throws Exception {
    String token = admin();
    var data =
        Map.of(
            "username",
            "doctor3",
            "password",
            "Doctor123!",
            "fullName",
            "Test Doctor",
            "specialization",
            "General",
            "email",
            "doctor3@example.com",
            "phone",
            "0900000003",
            "description",
            "Test");
    JsonNode d =
        json(
            mvc.perform(
                    post("/api/admin/doctors")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(data)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn());
    long id = d.get("id").asLong();
    String doctor = login("doctor3", "Doctor123!");
    mvc.perform(
            post("/api/admin/doctors")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(data)))
        .andExpect(status().isConflict());
    String profile =
        mapper.writeValueAsString(
            Map.of(
                "fullName",
                "Updated Doctor",
                "specialization",
                "General",
                "phone",
                "0900000003",
                "email",
                "doctor3@example.com",
                "description",
                "Updated"));
    mvc.perform(
            put("/api/doctor/profile")
                .header("Authorization", "Bearer " + doctor)
                .contentType(MediaType.APPLICATION_JSON)
                .content(profile))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Updated Doctor"));
    mvc.perform(get("/api/doctor/profile").header("Authorization", "Bearer " + doctor))
        .andExpect(status().isOk());
    mvc.perform(
            put("/api/admin/doctors/" + id)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(profile))
        .andExpect(status().isOk());
    mvc.perform(delete("/api/admin/doctors/" + id).header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/doctors/" + id)).andExpect(status().isNotFound());
    mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + doctor))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsString(
                        Map.of("username", "doctor3", "password", "Doctor123!"))))
        .andExpect(status().isUnauthorized());
    assertFalse(
        accounts.findByUsername("doctor3").orElseThrow().getPasswordHash().equals("Doctor123!"));
  }

  @Test
  void cannotDisableDoctorWithOpenAppointmentsAndAdminCanReadAll() throws Exception {
    String token = admin();
    JsonNode a = book(1, "09:00");
    assign(a, 1, "09:00", token).andExpect(status().isOk());
    mvc.perform(delete("/api/admin/doctors/1").header("Authorization", "Bearer " + token))
        .andExpect(status().isConflict());
    for (String path :
        List.of(
            "/api/admin/appointments",
            "/api/admin/appointments/" + a.get("id").asLong(),
            "/api/admin/doctors",
            "/api/admin/doctors/1",
            "/api/admin/doctors/1/appointments")) {
      mvc.perform(get(path).header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }
  }

  @Test
  void patientApiNotFoundAndEdgeCases() throws Exception {
    // 1. Tra cứu mã đặt lịch không tồn tại -> 404
    mvc.perform(get("/api/appointments/non-existent-code"))
        .andExpect(status().isNotFound());

    // 2. Hủy lịch với mã không tồn tại -> 404
    mvc.perform(delete("/api/appointments/non-existent-code"))
        .andExpect(status().isNotFound());

    // 3. Xem chi tiết bác sĩ không tồn tại -> 404
    mvc.perform(get("/api/doctors/9999"))
        .andExpect(status().isNotFound());

    // 4. Xem lịch trống với bác sĩ không tồn tại -> 404
    mvc.perform(get("/api/doctors/9999/available-slots?date=2030-01-02"))
        .andExpect(status().isNotFound());

    // 5. Xem lịch trống với serviceId không tồn tại -> 404
    mvc.perform(get("/api/doctors/1/available-slots?date=2030-01-02&serviceId=9999"))
        .andExpect(status().isNotFound());

    // 5b. Xem lịch trống với ngày ở quá khứ -> 400
    mvc.perform(get("/api/doctors/1/available-slots?date=2029-12-31"))
        .andExpect(status().isBadRequest());

    // 6. Đặt lịch với email sai định dạng -> 400
    var badEmail = booking(1, "09:00");
    badEmail.put("email", "not-a-valid-email");
    mvc.perform(
            post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(badEmail)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.email").exists());

    // 7. Đặt lịch với tên bệnh nhân để trống -> 400
    var blankName = booking(1, "09:00");
    blankName.put("patientName", "   ");
    mvc.perform(
            post("/api/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(blankName)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors.patientName").exists());

    // 8. Hủy lịch 2 lần liên tiếp -> lần 2 bị 409 Conflict
    JsonNode appt = book(1, "09:00");
    String code = appt.get("code").asText();
    mvc.perform(delete("/api/appointments/" + code))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"));
    mvc.perform(delete("/api/appointments/" + code))
        .andExpect(status().isConflict());

    // 9. Không thể hủy lịch sau khi giờ hẹn đã qua -> 409 Conflict
    String pastCode = UUID.randomUUID().toString();
    jdbc.update(
        "INSERT INTO appointments (code, patient_name, phone, service_id, preferred_date, preferred_time, status, created_at) "
            + "VALUES (?, 'Past Patient', '0912345678', 1, '2029-12-31', '09:00:00', 'PENDING', ?)",
        pastCode,
        NOW.toString());
    mvc.perform(delete("/api/appointments/" + pastCode))
        .andExpect(status().isConflict());
  }

  @Test
  void databaseTriggersProtectOverlapsEvenWhenServiceIsBypassed() throws Exception {
    String token = admin();
    JsonNode a = book(2, "09:00"), b = book(1, "10:00");
    assign(a, 1, "09:00", token).andExpect(status().isOk());
    assign(b, 1, "10:00", token).andExpect(status().isOk());
    assertThrows(
        DataAccessException.class,
        () ->
            jdbc.update(
                "UPDATE appointments SET start_time='09:30:00', end_time='10:00:00' WHERE id=?",
                b.get("id").asLong()));
    assertThrows(
        DataAccessException.class,
        () ->
            jdbc.update(
                "UPDATE appointments SET service_id=999999 WHERE id=?", b.get("id").asLong()));
  }

  @Test
  void simultaneousAssignmentsCannotReserveOverlappingDoctorSlots() throws Exception {
    String token = admin();
    JsonNode first = book(2, "09:00"), second = book(1, "09:30");
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var workers = Executors.newFixedThreadPool(2);
    try {
      var attempts = List.of(first, second).stream()
          .map(appointment -> workers.submit(() -> {
            ready.countDown();
            assertTrue(start.await(15, TimeUnit.SECONDS), "Start gate timed out");
            return assign(appointment, 1, appointment == first ? "09:00" : "09:30", token)
                .andReturn();
          }))
          .toList();
      assertTrue(ready.await(15, TimeUnit.SECONDS), "Both requests must be ready");
      start.countDown();
      MvcResult firstResult = attempts.get(0).get(15, TimeUnit.SECONDS);
      MvcResult secondResult = attempts.get(1).get(15, TimeUnit.SECONDS);
      var statuses = List.of(firstResult.getResponse().getStatus(), secondResult.getResponse().getStatus())
          .stream().sorted().toList();
      assertEquals(List.of(200, 409), statuses);
      assertEquals(1, jdbc.queryForObject(
          "SELECT COUNT(*) FROM appointments WHERE doctor_id=1 AND status='ASSIGNED'", Integer.class));
      assertEquals(1, jdbc.queryForObject(
          "SELECT COUNT(*) FROM appointments WHERE doctor_id IS NULL AND status='PENDING'", Integer.class));
      MvcResult conflict = firstResult.getResponse().getStatus() == 409 ? firstResult : secondResult;
      assertEquals(409, json(conflict).get("status").asInt());
      JsonNode pending = firstResult.getResponse().getStatus() == 409 ? first : second;
      assign(pending, 1, "10:00", token).andExpect(status().isOk());
    } finally {
      start.countDown();
      workers.shutdownNow();
    }
  }

  @Test
  void openApiSpecificationAndSwaggerUiAreAccessible() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.openapi").exists())
        .andExpect(jsonPath("$.info.title").value("Dental Clinic Management API"))
        .andExpect(jsonPath("$.paths['/api/admin/appointments']").exists())
        .andExpect(jsonPath("$.security").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/appointments'].post.security").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/auth/login'].post.security").doesNotExist())
        .andExpect(jsonPath("$.paths['/api/admin/appointments'].get.security[0].BearerAuth").isArray())
        .andExpect(jsonPath("$.paths['/api/doctor/profile'].get.security[0].BearerAuth").isArray())
        .andExpect(jsonPath("$.components.securitySchemes.BearerAuth").exists());

    mvc.perform(get("/swagger-ui/index.html"))
        .andExpect(status().isOk());
  }
}
