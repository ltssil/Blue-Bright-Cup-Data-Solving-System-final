package edu.university.academic.controller;

import edu.university.academic.utils.DBUtil;
import edu.university.academic.utils.RowProcessor;
import org.springframework.web.bind.annotation.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 最小化登录接口（不使用 Spring Security）
 * POST /api/login
 *   body: { "username": "...", "password": "..." }
 * 返回:
 *   success: true/false
 *   user: { uid, username, roles: [] }   // 当 success=true 时
 *   message: 错误说明（当 success=false 时）
 */
@RestController
@RequestMapping("/api")
@CrossOrigin
public class AuthController {

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String username = body.get("username") == null ? null : body.get("username").toString().trim();
        String password = body.get("password") == null ? null : body.get("password").toString();

        if (username == null || username.isEmpty() || password == null) {
            return Map.of("success", false, "message", "用户名或密码为空");
        }

        try {
            // 验证用户 明文密码匹配
            String sqlUser = "SELECT UID, username FROM users WHERE username = ? AND password = ?";
            List<Map<String, Object>> users = DBUtil.query(sqlUser, new Object[]{username, password}, new RowProcessor<Map<String, Object>>() {
                @Override
                public Map<String, Object> process(ResultSet rs) throws SQLException {
                    Map<String, Object> m = new HashMap<>();
                    m.put("uid", rs.getString("UID"));
                    m.put("username", rs.getString("username"));
                    return m;
                }
            });

            if (users == null || users.isEmpty()) {
                return Map.of("success", false, "message", "用户名或密码错误");
            }

            Map<String, Object> userRow = users.get(0);
            String uid = (String) userRow.get("uid");

            // 查询角色列表
            String sqlRoles = "SELECT r.roleName FROM sysrole r JOIN userrole ur ON r.roleId = ur.roleId WHERE ur.uid = ?";
            List<String> roles = DBUtil.query(sqlRoles, new Object[]{uid}, new RowProcessor<String>() {
                @Override
                public String process(ResultSet rs) throws SQLException {
                    return rs.getString("roleName");
                }
            });

            if (roles == null) roles = new ArrayList<>();

            Map<String, Object> user = new HashMap<>();
            user.put("uid", uid);
            user.put("username", username);
            user.put("roles", roles);

            return Map.of("success", true, "user", user);

        } catch (Exception ex) {
            ex.printStackTrace();
            return Map.of("success", false, "message", "登录异常: " + ex.getMessage());
        }
    }

    /**
     * 简单的登出接口 前端清 localStorage ，后端保留以备扩展
     */
    @PostMapping("/logout")
    public Map<String,Object> logout() {
        return Map.of("success", true);
    }
}
