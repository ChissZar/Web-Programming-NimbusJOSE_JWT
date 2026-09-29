# Project Demo JWT với Spring Boot 3 - Security 6 (Sử dụng Thư viện Nimbus JOSE + JWT)

## 📌 Giới thiệu
Project này được xây dựng dựa trên bài giảng **"Json Web Token - Demo JWT với Spring Boot 3 – Security 6"** của ThS. Nguyễn Hữu Trung (Trường ĐH Sư Phạm Kỹ Thuật TP.HCM - HCMUTE), với cải tiến theo yêu cầu:
👉 **Sử dụng thư viện Nimbus JOSE + JWT (`com.nimbusds:nimbus-jose-jwt`) thay thế hoàn toàn thư viện JJWT (`io.jsonwebtoken`).**

---

## 🛠 Công nghệ sử dụng
- **Java**: 26 / 21 / 17
- **Spring Boot**: 3.x / 4.x
- **Spring Security**: 6.x (Stateless Authentication với JWT Filter)
- **Spring Data JPA & Hibernate**
- **Thư viện JWT**: `com.nimbusds:nimbus-jose-jwt:10.9.1`
- **Cơ sở dữ liệu**: MySQL (mặc định theo Slide 21) & H2 in-memory (hỗ trợ test nhanh không cần cài DB)
- **Giao diện**: Thymeleaf + Bootstrap 5 + jQuery AJAX

---

## 📂 Cấu trúc Project
```text
JWT_springboot3/
├── src/main/java/vn/iotstar/
│   ├── configs/
│   │   ├── ApplicationConfiguration.java     # Cấu hình UserDetailsService, PasswordEncoder, AuthProvider
│   │   ├── SecurityConfiguration.java        # Cấu hình SecurityFilterChain, Stateless, CORS, Filter
│   │   └── GlobalExceptionHandler.java       # Xử lý tập trung các Exception (401, 403, 500)
│   ├── controllers/
│   │   ├── AuthenticationController.java     # REST API: /auth/signup, /auth/login
│   │   ├── UserController.java               # REST API: /users/me, /users/
│   │   └── AuthController.java               # Web Controller: /login, /user/profile (Thymeleaf)
│   ├── entity/
│   │   └── User.java                         # Entity users implements UserDetails
│   ├── exception/
│   │   ├── ExpiredJwtException.java          # Ngoại lệ token hết hạn
│   │   └── SignatureException.java           # Ngoại lệ chữ ký JWT không hợp lệ
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java      # OncePerRequestFilter trích xuất Bearer token
│   ├── models/
│   │   ├── LoginResponse.java                # DTO trả về { token, expiresIn }
│   │   ├── LoginUserModel.java               # DTO đăng nhập { email, password }
│   │   └── RegisterUserModel.java            # DTO đăng ký { email, password, fullName }
│   ├── repository/
│   │   └── UserRepository.java               # JpaRepository<User, Integer> findByEmail
│   ├── services/
│   │   ├── AuthenticationService.java        # Nghiệp vụ signup, authenticate
│   │   ├── JwtService.java                   # Xử lý JWT bằng NIMBUS JOSE + JWT
│   │   └── UserService.java                  # Lấy danh sách users
│   └── JwtSpringboot3Application.java        # Class main Spring Boot
├── src/main/resources/
│   ├── static/
│   │   ├── images/                           # Chứa ảnh đại diện
│   │   └── js/mainjs.js                      # Gọi REST API bằng AJAX (Slide 33)
│   ├── templates/
│   │   ├── login.html                        # Giao diện Đăng nhập (Slide 32)
│   │   └── profile.html                      # Giao diện Trang cá nhân (Slide 32)
│   ├── application.properties                # Cấu hình MySQL theo Slide 21
│   └── application-h2.properties             # Cấu hình H2 in-memory chạy ngay lập tức
└── pom.xml
```

---

## ⚡ Điểm khác biệt cốt lõi: Sử dụng Nimbus JOSE + JWT thay thế JJWT

| Thao tác | JJWT (`io.jsonwebtoken`) | Nimbus JOSE + JWT (`com.nimbusds`) |
| :--- | :--- | :--- |
| **Dependency** | `jjwt-api`, `jjwt-impl`, `jjwt-jackson` | `com.nimbusds:nimbus-jose-jwt` |
| **Khởi tạo Header** | Tự động sinh qua `Jwts.builder()` | `JWSHeader header = new JWSHeader(JWSAlgorithm.HS256)` |
| **Tạo Claims** | `Jwts.builder().subject(...).expiration(...)` | `JWTClaimsSet.Builder().subject(...).expirationTime(...)` |
| **Ký Token** | `Jwts.builder().signWith(key, HS256)` | `JWSSigner signer = new MACSigner(keyBytes); signedJWT.sign(signer)` |
| **Serialize ra chuỗi** | `.compact()` | `signedJWT.serialize()` |
| **Parse Token** | `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)` | `SignedJWT signedJWT = SignedJWT.parse(token); signedJWT.verify(verifier)` |
| **Trích xuất Claims** | `claims.getSubject()`, `claims.getExpiration()` | `claimsSet.getSubject()`, `claimsSet.getExpirationTime()` |

---

## 🚀 Hướng dẫn Chạy Ứng dụng

### Cách 1: Chạy với H2 Database (Không cần cài MySQL - Khuyến nghị để test ngay)
Mở Terminal trong thư mục `JWT_springboot3`:
```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=h2
```

### Cách 2: Chạy với MySQL (Theo cấu hình Slide 21)
1. Đảm bảo MySQL đang chạy trên port `3306`.
2. Tạo database:
   ```sql
   CREATE DATABASE jwt_springboot3;
   ```
3. Chạy lệnh:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

---

## 🧪 Hướng dẫn Kiểm thử

### 1. Kiểm thử tự động (Unit / Integration Tests)
Chạy lệnh test có sẵn:
```powershell
.\mvnw.cmd test
```
Toàn bộ 3 bài test xác thực Nimbus JWT, Đăng ký, Đăng nhập, trích xuất Subject/Claims đều tự động chạy và PASSED.

### 2. Kiểm thử qua Giao diện Web (AJAX)
1. Mở trình duyệt truy cập: `http://localhost:8005/login`
2. Chọn tab **"Đăng ký mới"** -> Điền:
   - Họ tên: `Nguyễn Hữu Trung`
   - Email: `trungnh@hcmute.edu.vn`
   - Mật khẩu: `123456`
   -> Nhấn **Đăng ký**.
3. Quay lại tab **"Đăng nhập"** -> Nhấn **Login**.
4. Trình duyệt tự động lưu Token vào `localStorage` và chuyển đến `http://localhost:8005/user/profile`.
5. AJAX tự động gửi Header: `Authorization: Bearer <token>` lên endpoint `/users/me` và hiển thị thông tin người dùng đã xác thực.

### 3. Kiểm thử bằng Postman (Theo Slide 28, 29, 31)

#### a) Đăng ký tài khoản:
- **POST** `http://localhost:8005/auth/signup`
- Body (raw JSON):
  ```json
  {
    "email": "trungnh@hcmute.edu.vn",
    "password": "123456",
    "fullName": "Nguyễn Hữu Trung"
  }
  ```

#### b) Đăng nhập sinh Nimbus JWT Token:
- **POST** `http://localhost:8005/auth/login`
- Body (raw JSON):
  ```json
  {
    "email": "trungnh@hcmute.edu.vn",
    "password": "123456"
  }
  ```
- Kết quả trả về:
  ```json
  {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0cnVuZ25oQGhjbXV0ZS5lZHUudm4iLCJpYXQiOjE3NDM...",
    "expiresIn": 3600000
  }
  ```

#### c) Lấy thông tin cá nhân:
- **GET** `http://localhost:8005/users/me`
- Headers:
  - `Authorization`: `Bearer <token_ở_bước_trên>`

#### d) Lấy danh sách người dùng:
- **GET** `http://localhost:8005/users/`
- Headers:
  - `Authorization`: `Bearer <token_ở_bước_trên>`
