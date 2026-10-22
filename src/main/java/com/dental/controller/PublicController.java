package com.dental.controller;

import com.dental.dto.*;
import com.dental.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "Public", description = "Các API công khai cho khách hàng (thông tin phòng khám, dịch vụ, bác sĩ, đặt hẹn, tra cứu)")
public class PublicController {
  private final CatalogService catalog;
  private final ScheduleService schedule;
  private final AppointmentService appointments;

  public PublicController(
      CatalogService catalog, ScheduleService schedule, AppointmentService appointments) {
    this.catalog = catalog;
    this.schedule = schedule;
    this.appointments = appointments;
  }

  @GetMapping("/clinic")
  @Operation(summary = "Xem thông tin phòng khám", description = "Lấy thông tin chung, địa chỉ, giờ mở/đóng cửa phòng khám")
  public Responses.ClinicView clinic() {
    return catalog.clinic();
  }

  @GetMapping("/services")
  @Operation(summary = "Xem danh mục dịch vụ nha khoa", description = "Lấy danh sách các dịch vụ nha khoa đang cung cấp, thời lượng và giá niêm yết")
  public List<Responses.ServiceView> services() {
    return catalog.services();
  }

  @GetMapping("/doctors")
  @Operation(summary = "Xem danh sách bác sĩ công khai", description = "Lấy danh sách các bác sĩ đang hoạt động trong phòng khám")
  public List<Responses.DoctorView> doctors() {
    return catalog.doctors();
  }

  @GetMapping("/doctors/{id}")
  @Operation(summary = "Xem thông tin chi tiết bác sĩ", description = "Lấy thông tin hồ sơ của 1 bác sĩ theo ID")
  public Responses.DoctorView doctor(@PathVariable Long id) {
    return catalog.doctor(id);
  }

  @GetMapping("/doctors/{id}/available-slots")
  @Operation(summary = "Xem các khung giờ còn trống của bác sĩ", description = "Tính toán các slot giờ 30 phút còn khả dụng của bác sĩ trong 1 ngày cụ thể")
  public Responses.AvailableSlots slots(
      @PathVariable Long id,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
      @RequestParam(required = false) Long serviceId) {
    return schedule.available(id, date, serviceId);
  }

  @PostMapping("/appointments")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Khách hàng đặt lịch hẹn", description = "Đặt lịch hẹn mới ở trạng thái PENDING mà không chỉ định bác sĩ. Hệ thống sinh mã tra cứu (booking code)")
  public Responses.AppointmentView book(@Valid @RequestBody Requests.Booking r) {
    return appointments.book(r);
  }

  @GetMapping("/appointments/{code}")
  @Operation(summary = "Khách hàng tra cứu lịch hẹn theo mã", description = "Tra cứu thông tin chi tiết lịch hẹn bằng booking code được cấp lúc đặt lịch")
  public Responses.AppointmentView lookup(@PathVariable String code) {
    return appointments.lookup(code);
  }

  @DeleteMapping("/appointments/{code}")
  @Operation(summary = "Khách hàng hủy lịch hẹn theo mã", description = "Hủy lịch hẹn nếu lịch đang ở trạng thái hợp lệ để hủy")
  public Responses.AppointmentView cancel(@PathVariable String code) {
    return appointments.cancel(code);
  }
}
