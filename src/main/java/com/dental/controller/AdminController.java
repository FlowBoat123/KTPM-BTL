package com.dental.controller;

import com.dental.dto.*;
import com.dental.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Các API quản trị hệ thống phòng khám, phân công lịch và quản lý bác sĩ")
@SecurityRequirement(name = "BearerAuth")
public class AdminController {
  private final DoctorService doctors;
  private final AppointmentService appointments;

  public AdminController(DoctorService doctors, AppointmentService appointments) {
    this.doctors = doctors;
    this.appointments = appointments;
  }

  @GetMapping("/appointments")
  @Operation(summary = "Xem toàn bộ lịch hẹn", description = "Lấy danh sách tất cả các lịch hẹn trong hệ thống theo thứ tự ngày giờ")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lấy danh sách lịch hẹn thành công"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
  })
  public List<Responses.AppointmentView> appointments() {
    return appointments.all();
  }

  @GetMapping("/appointments/{id}")
  @Operation(summary = "Xem chi tiết 1 lịch hẹn", description = "Lấy thông tin chi tiết của lịch hẹn theo ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lấy chi tiết lịch hẹn thành công"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin"),
      @ApiResponse(responseCode = "404", description = "Lịch hẹn không tồn tại")
  })
  public Responses.AppointmentView appointment(@PathVariable Long id) {
    return appointments.get(id);
  }

  @PatchMapping("/appointments/{id}/assign")
  @Operation(summary = "Phân công bác sĩ & giờ khám", description = "Admin chỉ định bác sĩ, ấn định ngày khám và giờ bắt đầu. Kiểm tra slot trống, bước 30 phút và không trùng lịch")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Phân công thành công, lịch chuyển sang ASSIGNED"),
      @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ / Thời gian ngoài giờ làm việc"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin"),
      @ApiResponse(responseCode = "404", description = "Lịch hẹn hoặc Bác sĩ không tồn tại"),
      @ApiResponse(responseCode = "409", description = "Bác sĩ bị trùng lịch hoặc lịch hẹn đã COMPLETED/CANCELLED")
  })
  public Responses.AppointmentView assign(
      @PathVariable Long id, @Valid @RequestBody Requests.Assignment r) {
    return appointments.assign(id, r);
  }

  @PatchMapping("/appointments/{id}/status")
  @Operation(summary = "Đổi trạng thái lịch hẹn", description = "Cập nhật trạng thái cuộc hẹn tuân thủ luồng State Machine")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cập nhật trạng thái thành công"),
      @ApiResponse(responseCode = "400", description = "Dữ liệu trạng thái không hợp lệ"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin"),
      @ApiResponse(responseCode = "404", description = "Lịch hẹn không tồn tại"),
      @ApiResponse(responseCode = "409", description = "Chuyển trạng thái vi phạm quy tắc State Machine")
  })
  public Responses.AppointmentView status(
      @PathVariable Long id, @Valid @RequestBody Requests.StatusChange r) {
    return appointments.change(id, r.status());
  }

  @GetMapping("/doctors")
  @Operation(summary = "Xem danh sách toàn bộ bác sĩ", description = "Lấy danh sách tất cả bác sĩ bao gồm cả tài khoản đã vô hiệu hóa")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lấy danh sách bác sĩ thành công"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin")
  })
  public List<Responses.DoctorView> doctors() {
    return doctors.all();
  }

  @PostMapping("/doctors")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Tạo tài khoản và hồ sơ bác sĩ mới", description = "Tạo tài khoản đăng nhập (Role DOCTOR) và thông tin hồ sơ bác sĩ mới")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "Tạo bác sĩ thành công"),
      @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ (mật khẩu < 8 ký tự, định dạng email/SĐT sai)"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin"),
      @ApiResponse(responseCode = "409", description = "Username đã tồn tại")
  })
  public Responses.DoctorView create(@Valid @RequestBody Requests.DoctorCreate r) {
    return doctors.create(r);
  }

  @GetMapping("/doctors/{id}")
  @Operation(summary = "Xem chi tiết hồ sơ bác sĩ", description = "Lấy thông tin hồ sơ bác sĩ theo ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lấy chi tiết bác sĩ thành công"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin"),
      @ApiResponse(responseCode = "404", description = "Bác sĩ không tồn tại")
  })
  public Responses.DoctorView doctor(@PathVariable Long id) {
    return doctors.get(id);
  }

  @PutMapping("/doctors/{id}")
  @Operation(summary = "Cập nhật hồ sơ bác sĩ", description = "Cập nhật thông tin họ tên, chuyên môn, SĐT, email, mô tả của bác sĩ")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cập nhật hồ sơ thành công"),
      @ApiResponse(responseCode = "400", description = "Dữ liệu cập nhật không hợp lệ"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin"),
      @ApiResponse(responseCode = "404", description = "Bác sĩ không tồn tại")
  })
  public Responses.DoctorView update(
      @PathVariable Long id, @Valid @RequestBody Requests.DoctorUpdate r) {
    return doctors.update(id, r);
  }

  @DeleteMapping("/doctors/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Vô hiệu hóa tài khoản bác sĩ", description = "Soft delete (active = false). Thu hồi quyền đăng nhập nhưng giữ nguyên lịch sử khám. Bị chặn nếu bác sĩ còn lịch hẹn đang mở")
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Vô hiệu hóa bác sĩ thành công"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin"),
      @ApiResponse(responseCode = "404", description = "Bác sĩ không tồn tại"),
      @ApiResponse(responseCode = "409", description = "Bác sĩ vẫn còn lịch hẹn đang mở (ASSIGNED hoặc CONFIRMED)")
  })
  public void disable(@PathVariable Long id) {
    doctors.disable(id);
  }

  @GetMapping("/doctors/{id}/appointments")
  @Operation(summary = "Xem lịch hẹn của 1 bác sĩ", description = "Lấy danh sách tất cả các lịch hẹn được phân công cho bác sĩ theo ID")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lấy danh sách lịch hẹn thành công"),
      @ApiResponse(responseCode = "401", description = "Chưa đăng nhập / Token không hợp lệ"),
      @ApiResponse(responseCode = "403", description = "Không có quyền Admin"),
      @ApiResponse(responseCode = "404", description = "Bác sĩ không tồn tại")
  })
  public List<Responses.AppointmentView> doctorAppointments(@PathVariable Long id) {
    return appointments.forDoctor(id);
  }
}
