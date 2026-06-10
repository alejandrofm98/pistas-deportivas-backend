package com.sportreserve.admin;

import com.sportreserve.court.CourtService;
import com.sportreserve.court.dto.CourtRequest;
import com.sportreserve.court.dto.CourtResponse;
import com.sportreserve.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CourtService courtService;
    private final Path uploadDir;

    public AdminController(CourtService courtService,
                           @Value("${app.upload.dir}") String uploadDirPath) {
        this.courtService = courtService;
        this.uploadDir = Paths.get(uploadDirPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDirPath, e);
        }
    }

    @GetMapping("/courts")
    public ResponseEntity<List<CourtResponse>> getAllCourts() {
        return ResponseEntity.ok(courtService.findAll());
    }

    @PostMapping("/courts")
    public ResponseEntity<CourtResponse> createCourt(@Valid @RequestBody CourtRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(courtService.create(request));
    }

    @PutMapping("/courts/{id}")
    public ResponseEntity<CourtResponse> updateCourt(
            @PathVariable UUID id,
            @Valid @RequestBody CourtRequest request) {
        return ResponseEntity.ok(courtService.update(id, request));
    }

    @DeleteMapping("/courts/{id}")
    public ResponseEntity<Void> deleteCourt(@PathVariable UUID id) {
        courtService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/courts/{id}/toggle")
    public ResponseEntity<Void> toggleCourt(@PathVariable UUID id) {
        courtService.toggleActive(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        if (file.isEmpty()) {
            throw new BusinessException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("Only image files are allowed");
        }

        try {
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;
            Path targetPath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath);

            String baseUrl = String.format("%s://%s:%d",
                request.getScheme(), request.getServerName(), request.getServerPort());

            return ResponseEntity.ok(Map.of("url", baseUrl + "/uploads/" + filename));
        } catch (IOException e) {
            throw new BusinessException("Failed to upload file: " + e.getMessage());
        }
    }
}
