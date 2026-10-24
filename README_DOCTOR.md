# BÁO CÁO KỸ THUẬT: PHÂN HỆ DOCTOR

**Dự án:** KTPM-BTL — Dental Clinic Backend  
**Phạm vi:** Phân hệ API dành cho bác sĩ  
**Công nghệ liên quan:** Java 17+, Spring Boot 3.5.x, Spring Data JPA, Spring Security, JWT, SQLite  
**Mục đích tài liệu:** Mô tả kiến trúc, luồng xử lý, hợp đồng API và các quy tắc nghiệp vụ của phân hệ Doctor để phục vụ đọc hiểu mã nguồn, tích hợp và bảo trì.

---

## 1. Tổng quan

Phân hệ Doctor cung cấp các chức năng cho tài khoản có vai trò `DOCTOR`: xem danh sách lịch được phân công, xem chi tiết lịch, cập nhật trạng thái lịch, xem hồ sơ cá nhân và cập nhật hồ sơ cá nhân.

Danh tính bác sĩ được xác định từ tài khoản đã xác thực. Client không được tự chọn `doctorId` để quyết định phạm vi dữ liệu truy cập. Khi request được xác thực, backend lấy thông tin `CurrentUser` từ Spring Security context, dùng `accountId` để truy xuất hồ sơ bác sĩ và kiểm tra quyền sở hữu lịch hẹn.

Các API này thuộc hệ thống có các vai trò chính sau:

| Vai trò | Phạm vi liên quan |
|---|---|
| `CUSTOMER` / khách chưa đăng nhập | Gửi yêu cầu đặt lịch và tra cứu/hủy lịch bằng mã đặt lịch theo các API công khai. Không truy cập `/api/doctor/**`. |
| `DOCTOR` | Chỉ xem và xử lý lịch được phân công cho chính mình; xem và cập nhật hồ sơ cá nhân. |
| `ADMIN` | Quản lý lịch hẹn, phân công/đổi bác sĩ và quản lý tài khoản bác sĩ qua `/api/admin/**`. |

## 2. Phạm vi API

| # | Method | Endpoint | Chức năng | Quyền yêu cầu |
|---:|---|---|---|---|
| 1 | `GET` | `/api/doctor/appointments` | Lấy danh sách lịch được phân công cho bác sĩ hiện tại. | `DOCTOR` |
| 2 | `GET` | `/api/doctor/appointments/{id}` | Lấy chi tiết một lịch được phân công cho bác sĩ hiện tại. | `DOCTOR` |
| 3 | `PATCH` | `/api/doctor/appointments/{id}/status` | Chuyển trạng thái lịch theo quy tắc nghiệp vụ. | `DOCTOR` |
| 4 | `GET` | `/api/doctor/profile` | Lấy hồ sơ bác sĩ hiện tại. | `DOCTOR` |
| 5 | `PUT` | `/api/doctor/profile` | Cập nhật hồ sơ bác sĩ hiện tại. | `DOCTOR` |

Phân công hoặc đổi bác sĩ, ngày và giờ khám thuộc API Admin `PATCH /api/admin/appointments/{id}/assign`; không thuộc trách nhiệm của API Doctor.

## 3. Kiến trúc và luồng xử lý

### 3.1. Luồng request tổng thể

```text
HTTP request
    |
    v
Spring Security / JwtAuthenticationFilter
    |  Xác minh Bearer token và tạo principal CurrentUser
    v
DoctorController
    |  Nhận path/body và chuyển dữ liệu đã xác thực cho service
    v
DoctorService / AppointmentService
    |  Kiểm tra nghiệp vụ, quyền sở hữu và trạng thái
    v
DoctorRepository / AppointmentRepository
    |  Truy vấn hoặc lưu thay đổi
    v
Entity + SQLite
    |
    v
ViewMapper -> Responses DTO -> JSON response
```

### 3.2. Trách nhiệm các thành phần

| Thành phần | Trách nhiệm trong phân hệ Doctor |
|---|---|
| `controller/DoctorController.java` | Khai báo route HTTP, tiếp nhận path/body, nhận `CurrentUser` và gọi service. Controller không nên chứa các quy tắc nghiệp vụ phức tạp. |
| `service/DoctorService.java` | Tra cứu hồ sơ theo `accountId` và cập nhật thông tin hồ sơ bác sĩ. |
| `service/AppointmentService.java` | Lấy danh sách/chi tiết lịch thuộc bác sĩ, xác minh quyền sở hữu và xử lý chuyển trạng thái. |
| `repository/DoctorRepository.java` | Truy vấn hồ sơ bác sĩ, bao gồm tra cứu theo tài khoản liên kết. |
| `repository/AppointmentRepository.java` | Truy vấn lịch hẹn, bao gồm danh sách lịch theo bác sĩ được sắp xếp theo ngày/giờ. |
| `entity/Doctor.java` | Biểu diễn hồ sơ bác sĩ và quan hệ với `Account`. |
| `entity/Appointment.java` | Biểu diễn lịch hẹn, dữ liệu bệnh nhân, dịch vụ, bác sĩ được phân công và trạng thái. |
| `entity/AppointmentStatus.java` | Tập các giá trị trạng thái lịch hẹn. |
| `security/CurrentUser.java` | Principal của request đã xác thực; chứa các thông tin tài khoản như `accountId`, `username`, `role` và `sessionId`. |
| `security/JwtAuthenticationFilter.java` | Đọc Bearer token, xác thực phiên và đưa principal vào Spring Security context. |
| `config/SecurityConfig.java` | Khai báo quy tắc truy cập theo đường dẫn và vai trò; `/api/doctor/**` dành cho `ROLE_DOCTOR`. |
| `dto/Requests.java` | Các DTO request và quy tắc validation đầu vào. |
| `dto/Responses.java` | Các DTO response công khai của API; không trả trực tiếp entity hoặc dữ liệu mật khẩu. |
| `dto/ViewMapper.java` | Chuyển Entity thành DTO response. |
| `exception/ApiException.java` và exception handler | Biểu diễn lỗi nghiệp vụ và chuẩn hóa response lỗi. |
| `src/test/java/com/dental/ApiIntegrationTest.java` | Kiểm thử tích hợp API qua HTTP, gồm phân quyền và các quy tắc nghiệp vụ. |

Tên lớp và vị trí tệp cần được đối chiếu với repository ở nhánh đang triển khai nếu cấu trúc mã nguồn thay đổi.

## 4. Xác thực và phân quyền

### 4.1. Xác định tài khoản hiện tại

1. Người dùng đăng nhập qua `POST /api/auth/login` bằng `username` và `password`.
2. Backend trả `accessToken` nếu thông tin đăng nhập hợp lệ.
3. Client gửi token trong header `Authorization: Bearer <accessToken>` ở các API cần xác thực.
4. `JwtAuthenticationFilter` kiểm tra token/phiên và tạo principal `CurrentUser`.
5. `SecurityConfig` yêu cầu vai trò `ROLE_DOCTOR` đối với `/api/doctor/**`.
6. Controller lấy `accountId` từ principal và truyền xuống service.
7. Service truy xuất `Doctor` liên kết với `accountId`; các truy vấn lịch tiếp tục giới hạn theo bác sĩ đó.

Username được sử dụng để đăng nhập. Email trong hồ sơ bác sĩ là thông tin liên hệ, không phải khóa liên kết hồ sơ với tài khoản.

### 4.2. Nguyên tắc bảo mật dữ liệu

- ID tài khoản dùng để xác định người gọi phải lấy từ principal đã xác thực, không lấy từ body/query do client tự gửi.
- API chi tiết và cập nhật lịch phải xác minh lịch thuộc bác sĩ hiện tại trước khi trả dữ liệu hoặc thay đổi trạng thái.
- Token sai, hết hạn hoặc đã thu hồi dẫn đến lỗi xác thực; người dùng sai vai trò không được truy cập API Doctor.
- Dữ liệu trả về được chuyển qua DTO; không trả trực tiếp entity chứa thông tin nội bộ.

## 5. Luồng xử lý từng API

### 5.1. Danh sách lịch hẹn

**Endpoint:** `GET /api/doctor/appointments`

**Luồng xử lý:**

1. `DoctorController` nhận `CurrentUser` và lấy `accountId`.
2. Controller gọi `AppointmentService.mine(accountId)`.
3. Service xác định hồ sơ bác sĩ thông qua `DoctorService`.
4. Service gọi repository để lấy các lịch theo `doctorId`, sắp xếp theo ngày và giờ khám.
5. Mỗi entity lịch được ánh xạ thành `Responses.AppointmentView` rồi trả về dạng JSON array.

**Hành vi kỳ vọng:** chỉ trả lịch được phân công cho bác sĩ đang đăng nhập. Nếu không có lịch, trả danh sách rỗng; không yêu cầu client truyền `doctorId`.

### 5.2. Chi tiết lịch hẹn

**Endpoint:** `GET /api/doctor/appointments/{id}`

**Luồng xử lý:**

1. Controller nhận ID lịch và `accountId` hiện tại.
2. Service tìm lịch theo ID.
3. Service kiểm tra lịch đã được phân công và tài khoản của bác sĩ được phân công trùng với tài khoản hiện tại.
4. Nếu điều kiện hợp lệ, entity được ánh xạ sang `Responses.AppointmentView`.

**Các trường hợp lỗi:**

- Lịch không tồn tại: `404 Not Found`.
- Lịch thuộc bác sĩ khác hoặc chưa có bác sĩ được phân công: `403 Forbidden` theo mô tả triển khai hiện tại.

### 5.3. Cập nhật trạng thái lịch hẹn

**Endpoint:** `PATCH /api/doctor/appointments/{id}/status`

**Request mẫu:**

```json
{
  "status": "CONFIRMED"
}
```

**Luồng xử lý:**

1. Framework chuyển JSON sang DTO `Requests.StatusChange` và thực hiện validation/parse enum.
2. Controller gọi `AppointmentService.changeMine(accountId, id, status)`.
3. Service xác minh lịch thuộc bác sĩ hiện tại.
4. Service kiểm tra cặp trạng thái hiện tại và trạng thái đích.
5. Nếu chuyển trạng thái hợp lệ, service cập nhật entity và lưu thay đổi qua repository.
6. Nếu chuyển trạng thái không hợp lệ, backend trả lỗi nghiệp vụ `409 Conflict`.

**Luồng trạng thái theo README chính:**

| Trạng thái hiện tại | Chuyển trạng thái được phép |
|---|---|
| `PENDING` | `CANCELLED` theo luồng nghiệp vụ chung; lịch chưa được phân công không thuộc phạm vi của bác sĩ. |
| `ASSIGNED` | `CONFIRMED`, `COMPLETED`, `CANCELLED` |
| `CONFIRMED` | `COMPLETED`, `CANCELLED` |
| `COMPLETED` | Không được mở lại hoặc phân công lại. |
| `CANCELLED` | Không được mở lại hoặc phân công lại. |

`ASSIGNED` được đặt bởi API phân công của Admin. Khi Admin phân công lại một lịch đang `ASSIGNED` hoặc `CONFIRMED`, lịch trở về `ASSIGNED` để bác sĩ mới xác nhận lại. Bác sĩ không tự phân công hay đổi lịch khám thực tế.

### 5.4. Xem hồ sơ cá nhân

**Endpoint:** `GET /api/doctor/profile`

**Luồng xử lý:**

1. Controller lấy `accountId` từ `CurrentUser`.
2. `DoctorService.profile(accountId)` tìm hồ sơ liên kết với tài khoản.
3. `ViewMapper` chuyển `Doctor` thành `Responses.DoctorView`.
4. Controller trả DTO dưới dạng JSON.

Theo DTO được mô tả trong code, hồ sơ gồm các trường như `id`, `fullName`, `specialization`, `phone`, `email`, `description` và `active`.

### 5.5. Cập nhật hồ sơ cá nhân

**Endpoint:** `PUT /api/doctor/profile`

**Request mẫu:**

```json
{
  "fullName": "Bac si Nguyen Van B",
  "specialization": "Nha khoa tong quat",
  "phone": "0901234567",
  "email": "doctor@example.com",
  "description": "Kham va tu van nha khoa"
}
```

**Luồng xử lý:**

1. Backend validate JSON theo `Requests.DoctorUpdate`.
2. `DoctorService.updateProfile(accountId, request)` xác định hồ sơ từ tài khoản hiện tại.
3. Service áp dụng các trường trong request lên entity.
4. Repository lưu hồ sơ; response được tạo thông qua `ViewMapper`.

`PUT` hiện sử dụng DTO cập nhật hồ sơ hoàn chỉnh, không phải bản cập nhật từng phần. `fullName` và `specialization` là các trường bắt buộc theo mô tả DTO. Các trường không bắt buộc có thể bị gán `null` nếu bị bỏ khỏi request, vì vậy request cần gửi đầy đủ các trường hồ sơ muốn giữ lại.

**Điểm cần xác nhận về nghiệp vụ:** DTO cập nhật hồ sơ được dùng chung với chức năng Admin và hiện cho phép cập nhật `specialization`. Nếu quy định sản phẩm yêu cầu chỉ Admin được sửa chuyên môn, cần tách DTO hoặc bổ sung kiểm tra quyền phù hợp.

## 6. Mô hình dữ liệu liên quan

| Entity / bảng | Vai trò trong phân hệ Doctor |
|---|---|
| `accounts` / `Account` | Tài khoản đăng nhập, gồm `username`, `passwordHash`, `role`, `active` và quan hệ phiên xác thực. Mật khẩu được lưu dưới dạng BCrypt hash. |
| `doctors` / `Doctor` | Hồ sơ bác sĩ, liên kết một-một với `Account`. |
| `appointments` / `Appointment` | Lưu thông tin bệnh nhân trực tiếp trong lịch, dịch vụ, bác sĩ được phân công, ngày/giờ và trạng thái. Bác sĩ có thể chưa được gán trước khi Admin phân công. |
| `dental_services` / dịch vụ | Dịch vụ gắn với lịch hẹn; thời lượng dịch vụ ảnh hưởng đến việc tính/kiểm tra khung giờ. |
| `login_sessions` | Phiên đăng nhập dùng để thu hồi token khi đăng xuất và kiểm tra trạng thái phiên. |

Theo cấu trúc hiện tại được mô tả trong README dự án, thông tin bệnh nhân được lưu trực tiếp trong `Appointment`; không cần tạo entity/bảng `Patient` riêng chỉ để phục vụ năm API Doctor.

## 7. DTO, validation và response lỗi

DTO tách hợp đồng API khỏi cấu trúc entity trong database. Request DTO kiểm soát dữ liệu đầu vào; response DTO giới hạn các trường được công khai. `ViewMapper` chịu trách nhiệm chuyển đổi từ entity sang response DTO.

| HTTP status | Ý nghĩa trong phân hệ |
|---|---|
| `200 OK` | Đọc dữ liệu hoặc cập nhật thành công. |
| `400 Bad Request` | JSON sai định dạng, trường bắt buộc thiếu hoặc validation thất bại. |
| `401 Unauthorized` | Thiếu token hoặc token không hợp lệ, hết hạn hay đã bị thu hồi. |
| `403 Forbidden` | Sai vai trò hoặc bác sĩ truy cập lịch thuộc bác sĩ khác. |
| `404 Not Found` | Không tìm thấy lịch hoặc hồ sơ bác sĩ. |
| `409 Conflict` | Chuyển trạng thái không hợp lệ hoặc xảy ra xung đột nghiệp vụ. |

Response lỗi chung theo README chính có các trường `timestamp`, `status`, `message` và `errors`; `errors` chứa thông tin validation theo trường khi có.

## 8. Cài đặt và gọi thử API

### 8.1. Khởi động

Yêu cầu JDK 17 trở lên. Tại thư mục gốc repository, chạy trên Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Profile `demo` mặc định tạo các tài khoản mẫu khi chưa tồn tại:

| Username | Password | Vai trò |
|---|---|---|
| `doctor1` | `Doctor123!` | `DOCTOR` |
| `doctor2` | `Doctor123!` | `DOCTOR` |
| `admin` | `Admin123!` | `ADMIN` |

Các thông tin trên chỉ dùng cho môi trường demo/local.

### 8.2. Đăng nhập

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{"username":"doctor1","password":"Doctor123!"}
```

Sao chép `accessToken` từ response để gửi trong header `Authorization: Bearer <accessToken>` ở các request tiếp theo.

### 8.3. Xem hồ sơ và danh sách lịch

```http
GET http://localhost:8080/api/doctor/profile
Authorization: Bearer <accessToken>
```

```http
GET http://localhost:8080/api/doctor/appointments
Authorization: Bearer <accessToken>
```

### 8.4. Xem chi tiết và cập nhật trạng thái

```http
GET http://localhost:8080/api/doctor/appointments/{id}
Authorization: Bearer <accessToken>
```

```http
PATCH http://localhost:8080/api/doctor/appointments/{id}/status
Authorization: Bearer <accessToken>
Content-Type: application/json

{"status":"CONFIRMED"}
```

Request chuyển trạng thái chỉ thành công khi lịch thực sự được phân công cho tài khoản `doctor1` và trạng thái hiện tại cho phép chuyển sang `CONFIRMED`. ID trong ví dụ cần thay bằng ID thực tế từ response của luồng đặt lịch/phân công.

### 8.5. Cập nhật hồ sơ

```http
PUT http://localhost:8080/api/doctor/profile
Authorization: Bearer <accessToken>
Content-Type: application/json

{
  "fullName": "Bac si Nguyen Van B",
  "specialization": "Nha khoa tong quat",
  "phone": "0901234567",
  "email": "doctor1@example.com",
  "description": "Tu van va dieu tri"
}
```

Swagger UI có tại `http://localhost:8080/swagger-ui/index.html` khi ứng dụng đang chạy.

## 9. Kiểm thử

Kiểm thử tích hợp của backend nằm tại `src/test/java/com/dental/ApiIntegrationTest.java`. Theo README chính, test sử dụng SQLite tạm và thời gian cố định, không sửa database demo đang chạy.

Chạy toàn bộ kiểm thử và xác minh build trên Windows:

```powershell
.\mvnw.cmd verify
```

Các trường hợp cần được kiểm tra đối với phân hệ Doctor:

- Thiếu token, token sai/hết hạn hoặc tài khoản không có vai trò `DOCTOR`.
- Danh sách lịch chỉ bao gồm các lịch thuộc bác sĩ đang đăng nhập.
- Bác sĩ không xem/cập nhật được lịch của bác sĩ khác.
- Các chuyển trạng thái hợp lệ như `ASSIGNED -> CONFIRMED -> COMPLETED`.
- Lịch có thể được hủy theo quy tắc nghiệp vụ; `COMPLETED` và `CANCELLED` không được mở lại.
- Request cập nhật hồ sơ không hợp lệ bị từ chối.
- Tài khoản Admin không thể sử dụng API dành riêng cho Doctor bằng token Admin.

## 10. Các điểm cần hoàn thiện hoặc thống nhất

1. **Swagger status endpoint:** mô tả API cần ghi rõ cả `CANCELLED` bên cạnh `CONFIRMED` và `COMPLETED`.
2. **Kiểm thử hủy lịch:** cần có test trực tiếp xác minh bác sĩ hủy được lịch thuộc mình và không thể mở lại lịch đã hủy.
3. **Quyền sửa chuyên môn:** cần thống nhất liệu bác sĩ có được tự cập nhật `specialization` hay chỉ Admin được phép.
4. **Hợp đồng response:** danh sách lịch hiện được mô tả là JSON array trực tiếp; không nên giả định response được bọc bằng `{ "data": [...] }` nếu code không định nghĩa wrapper đó.
5. **Đồng bộ tài liệu với code:** mọi thay đổi về DTO, mã HTTP, trạng thái hoặc phân quyền nên được cập nhật đồng thời trong tài liệu và kiểm thử.

---

**Kết luận:** Phân hệ Doctor là lớp API phía bác sĩ trong kiến trúc nhiều tầng của Dental Clinic Backend. Việc truy xuất dữ liệu được ràng buộc bởi tài khoản đã xác thực và quan hệ phân công lịch hẹn; nghiệp vụ lịch nằm chủ yếu trong `AppointmentService`, còn quản lý hồ sơ nằm trong `DoctorService`. Tài liệu này cần được cập nhật cùng các thay đổi mã nguồn để duy trì tính nhất quán giữa thiết kế và triển khai.

## 11. Patch bổ sung và cách áp dụng

Tệp `doctor-module-improvements.patch` là bản chênh lệch mã nguồn (unified diff), không phải file Java độc lập và không cần chạy trực tiếp bằng Java. Patch chỉ chứa các thay đổi sau:

1. Cập nhật mô tả Swagger của `PATCH /api/doctor/appointments/{id}/status` để liệt kê trạng thái `CANCELLED`.
2. Bổ sung test tích hợp cho trường hợp bác sĩ chỉ nhìn thấy lịch thuộc phạm vi của mình và trường hợp bác sĩ hủy lịch đã phân công nhưng không được mở lại lịch đã hủy.

Patch **không tạo mới 5 API Doctor** và không thay thế các lớp Controller/Service/Repository hiện có. Đây là phần bổ sung nhỏ nhằm đồng bộ tài liệu Swagger và tăng độ bao phủ kiểm thử.

### Vị trí đặt tệp

Đặt `doctor-module-improvements.patch` tại thư mục gốc repository — cùng cấp với `pom.xml`, `mvnw.cmd` và thư mục `src`. Đường dẫn trong patch là đường dẫn tương đối từ thư mục gốc, ví dụ `src/main/java/com/dental/controller/DoctorController.java`.

### Quy trình áp dụng trên Windows PowerShell

```powershell
git status
git checkout -b chore/doctor-module-improvements
git apply --check .\doctor-module-improvements.patch
git apply .\doctor-module-improvements.patch
git diff --check
.\mvnw.cmd verify
```

`git apply --check` kiểm tra patch có khớp với mã nguồn hiện tại mà chưa thay đổi file; `git apply` mới thực sự áp dụng thay đổi; `git diff --check` kiểm tra lỗi whitespace; `mvnw verify` chạy các bước kiểm thử và xác minh build. Nên tạo nhánh riêng và kiểm tra `git status` trước khi áp dụng để tránh trộn với thay đổi đang làm.

Nếu `git apply --check` báo lỗi, không nên ép áp dụng. Nguyên nhân thường là mã nguồn trên nhánh hiện tại đã khác ngữ cảnh mà patch được tạo ra; khi đó cần đối chiếu từng thay đổi thủ công hoặc tạo patch mới dựa trên phiên bản repository đang dùng.

Patch đã được kiểm tra định dạng diff, nhưng chưa được áp dụng và chạy `verify` trên bản repository tại máy người phát triển trong quá trình tạo tài liệu này. Vì vậy, cần thực hiện các lệnh trên trong repository thực tế để xác nhận kết quả.
