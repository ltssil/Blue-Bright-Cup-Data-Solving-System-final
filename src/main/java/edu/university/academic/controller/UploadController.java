package edu.university.academic.controller;

import edu.university.academic.operation.ExcelImportOperation;
import edu.university.academic.utils.DBUtil;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;

@RestController
//@Controller + @ResponsBody 返回字符串
@RequestMapping("/api")
@CrossOrigin
// 解决 CORS 跨域资源共享
public class UploadController {

    /**
     * 使用 DELETE 来清空表
     */
    private void clearTable(String tableName) {
        try {
            DBUtil.update("DELETE FROM " + tableName, new Object[]{});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/upload/students") // @RequestMapping(methods = Request.POST)
    public Map<String, Object> uploadStudents(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "force", required = false) String force) {
        if (file == null || file.isEmpty()) {
            return Map.of("success", false, "message", "文件为空");
        }
        try (InputStream is = file.getInputStream()) {
            if ("true".equalsIgnoreCase(force)) {
                clearTable("students");
            }
            return ExcelImportOperation.importStudents(is);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/upload/teachers")
    public Map<String, Object> uploadTeachers(@RequestParam("file") MultipartFile file,
                                              @RequestParam(value = "force", required = false) String force) {
        if (file == null || file.isEmpty()) {
            return Map.of("success", false, "message", "文件为空");
        }
        try (InputStream is = file.getInputStream()) {
            if ("true".equalsIgnoreCase(force)) {
                clearTable("teachers");
            }
            return ExcelImportOperation.importTeachers(is);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/upload/sign")
    public Map<String, Object> uploadSign(@RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "force", required = false) String force) {
        if (file == null || file.isEmpty()) {
            return Map.of("success", false, "message", "文件为空");
        }
        try (InputStream is = file.getInputStream()) {
            if ("true".equalsIgnoreCase(force)) {
                clearTable("sign");
                // clearTable("conflict");
            }
            return ExcelImportOperation.importSign(is);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }

    @PostMapping("/upload/award")
    public Map<String, Object> uploadAward(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "force", required = false) String force) {
        if (file == null || file.isEmpty()) {
            return Map.of("success", false, "message", "文件为空");
        }
        try (InputStream is = file.getInputStream()) {
            if ("true".equalsIgnoreCase(force)) {
                clearTable("award");
            }
            return ExcelImportOperation.importAwardAndExtractConflicts(is);
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("success", false, "message", e.getMessage());
        }
    }
}