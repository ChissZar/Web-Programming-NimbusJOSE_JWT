$(document).ready(function() {
    // Hiển thị token lưu trong localStorage nếu có
    if ($('#token-display').length && localStorage.token) {
        $('#token-display').text(localStorage.token);
    }

    // Hiển thị thông tin người dùng đăng nhập thành công (/users/me)
    if ($('#profile').length) {
        if (!localStorage.token) {
            $('#feedback').html('<div class="alert alert-warning mt-3">Bạn chưa đăng nhập! Đang chuyển hướng về trang đăng nhập...</div>');
            setTimeout(function() {
                window.location.href = "/login";
            }, 1200);
            return;
        }

        $.ajax({
            type: 'GET',
            url: '/users/me',
            dataType: 'json',
            contentType: "application/json; charset=utf-8",
            beforeSend: function(xhr) {
                if (localStorage.token) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.token);
                }
            },
            success: function(data) {
                $('#profile').html(data.fullName || data.username);
                $('#user-email-display').text(data.email || data.username);
                if (data.images && document.getElementById("images")) {
                    document.getElementById("images").src = data.images;
                }
                console.log("SUCCESS: ", data);
            },
            error: function(e) {
                var json = e.responseText || "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.";
                $('#feedback').html('<div class="alert alert-danger mt-3">Lỗi xác thực: ' + json + '</div>');
                console.log("ERROR: ", e);
            }
        });
    }

    // Hàm đăng xuất
    $('#logout').click(function() {
        localStorage.clear();
        window.location.href = "/login";
    });

    // Hàm Login (Chính xác theo Slide 33)
    $('#login').click(function() {
        var email = document.getElementById('email').value;
        var password = document.getElementById('password').value;
        if (!email || !password) {
            alert("Vui lòng điền đầy đủ email và password!");
            return;
        }

        var basicInfo = JSON.stringify({
            email: email,
            password: password
        });

        $.ajax({
            type: "POST",
            url: "/auth/login",
            dataType: 'json',
            contentType: "application/json; charset=utf-8",
            data: basicInfo,
            success: function(data) {
                localStorage.token = data.token;
                window.location.href = "/user/profile";
            },
            error: function(xhr) {
                if (xhr.status === 0) {
                    alert("Đăng nhập thất bại: Không kết nối được đến server (hãy kiểm tra Spring Boot có đang chạy tại cổng 8005 không).");
                    return;
                }
                var msg = "Tài khoản hoặc mật khẩu không chính xác.";
                if (xhr.responseJSON && (xhr.responseJSON.description || xhr.responseJSON.detail)) {
                    msg = xhr.responseJSON.description || xhr.responseJSON.detail;
                } else if (xhr.responseText) {
                    msg = xhr.responseText;
                }
                alert("Đăng nhập thất bại: " + msg);
            }
        });
    });

    // Hàm Đăng ký (Register)
    $('#btn-register').click(function() {
        var fullName = $('#reg-fullname').val();
        var email = $('#reg-email').val();
        var password = $('#reg-password').val();

        if (!fullName || !email || !password) {
            alert("Vui lòng nhập đầy đủ họ tên, email và mật khẩu!");
            return;
        }

        var payload = JSON.stringify({
            fullName: fullName,
            email: email,
            password: password
        });

        $.ajax({
            type: "POST",
            url: "/auth/signup",
            dataType: 'json',
            contentType: "application/json; charset=utf-8",
            data: payload,
            success: function(data) {
                alert("Đăng ký thành công! Hãy đăng nhập với email: " + data.email);
                $('#login-tab').tab('show');
                $('#email').val(data.email);
                $('#password').val(password);
            },
            error: function(xhr) {
                if (xhr.status === 0) {
                    alert("Đăng ký thất bại: Không kết nối được đến server (hãy kiểm tra Spring Boot có đang chạy tại cổng 8005 không).");
                    return;
                }
                var msg = "Lỗi không xác định";
                if (xhr.responseJSON && (xhr.responseJSON.description || xhr.responseJSON.detail)) {
                    msg = xhr.responseJSON.description || xhr.responseJSON.detail;
                } else if (xhr.responseText) {
                    msg = xhr.responseText;
                }
                alert("Đăng ký thất bại: " + msg);
            }
        });
    });

    // Hàm Tải danh sách người dùng (/users)
    $('#btn-load-users').click(function() {
        $.ajax({
            type: 'GET',
            url: '/users/',
            dataType: 'json',
            contentType: "application/json; charset=utf-8",
            beforeSend: function(xhr) {
                if (localStorage.token) {
                    xhr.setRequestHeader('Authorization', 'Bearer ' + localStorage.token);
                }
            },
            success: function(users) {
                var html = '<ul class="list-group list-group-flush mt-2">';
                users.forEach(function(u) {
                    html += '<li class="list-group-item d-flex justify-content-between align-items-center">' +
                            '<div><strong>' + (u.fullName || 'No name') + '</strong> (' + u.email + ')</div>' +
                            '<span class="badge bg-secondary">ID: ' + u.id + '</span>' +
                            '</li>';
                });
                html += '</ul>';
                $('#users-list-container').html(html);
            },
            error: function(xhr) {
                $('#users-list-container').html('<div class="text-danger">Không tải được danh sách: ' + (xhr.responseText || 'Lỗi xác thực') + '</div>');
            }
        });
    });
});
