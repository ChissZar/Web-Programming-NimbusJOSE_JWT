package vn.iotstar;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.iotstar.entity.User;
import vn.iotstar.models.LoginUserModel;
import vn.iotstar.models.RegisterUserModel;
import vn.iotstar.services.AuthenticationService;
import vn.iotstar.services.JwtService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("h2")
class JwtSpringboot3ApplicationTests {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationService authenticationService;

    @Test
    void contextLoads() {
        assertNotNull(jwtService, "JwtService bean should be loaded");
        assertNotNull(authenticationService, "AuthenticationService bean should be loaded");
    }

    @Test
    @DisplayName("Kiểm tra sinh và xác thực JWT token bằng thư viện Nimbus JOSE + JWT")
    void testNimbusJwtGenerationAndValidation() {
        // Tạo User giả lập
        User user = new User();
        user.setEmail("trungnh@hcmute.edu.vn");
        user.setFullName("ThS. Nguyễn Hữu Trung");
        user.setPassword("123456");

        // 1. Sinh token bằng Nimbus JOSE + JWT
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", "ROLE_ADMIN");
        extraClaims.put("authorities", "ADMIN");

        String token = jwtService.generateToken(extraClaims, user);

        assertNotNull(token, "Token không được null");
        assertFalse(token.isEmpty(), "Token không được rỗng");

        // Cấu trúc chuỗi JWT phải gồm đúng 3 phần: header.payload.signature
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT phải gồm đúng 3 phần: Header, Payload, Signature");

        // 2. Trích xuất thông tin Subject (Username/Email) từ token
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("trungnh@hcmute.edu.vn", extractedUsername, "Username trích xuất từ token phải khớp");

        // 3. Kiểm tra tính hợp lệ của token
        boolean isValid = jwtService.isTokenValid(token, user);
        assertTrue(isValid, "Nimbus JWT token phải hợp lệ với thông tin user tương ứng");

        // 4. Kiểm tra thời gian hết hạn
        assertTrue(jwtService.getExpirationTime() > 0, "Expiration time phải lớn hơn 0");
    }

    @Test
    @DisplayName("Kiểm tra quy trình Đăng ký (signup) và Đăng nhập (authenticate)")
    void testSignupAndAuthenticateFlow() {
        String testEmail = "sv_" + System.currentTimeMillis() + "@hcmute.edu.vn";

        // 1. Đăng ký tài khoản mới
        RegisterUserModel regModel = new RegisterUserModel(testEmail, "mypassword123", "Sinh Vien Test");
        User registeredUser = authenticationService.signup(regModel);

        assertNotNull(registeredUser.getId(), "User ID phải được tự sinh sau khi lưu");
        assertEquals(testEmail, registeredUser.getEmail());
        assertEquals("Sinh Vien Test", registeredUser.getFullName());
        assertNotEquals("mypassword123", registeredUser.getPassword(), "Mật khẩu phải được mã hóa BCrypt");

        // 2. Đăng nhập thành công với thông tin đúng
        LoginUserModel loginModel = new LoginUserModel(testEmail, "mypassword123");
        User authenticatedUser = authenticationService.authenticate(loginModel);

        assertNotNull(authenticatedUser);
        assertEquals(testEmail, authenticatedUser.getEmail());

        // 3. Sinh token Nimbus cho user đã đăng nhập
        String token = jwtService.generateToken(authenticatedUser);
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, authenticatedUser));
    }
}
