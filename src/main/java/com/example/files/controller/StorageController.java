package com.example.files.controller;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;


@RestController
@RequestMapping("/api/storage")
public class StorageController {

    private static final Path BASE_DIR = Paths.get("/data/uploads");
    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024; // 50MB

    private static final Set<String> VIEWABLE_EXTENSIONS = Set.of(
            "txt","md","log",
            "jpg","jpeg","png","gif","webp","svg",
            "pdf","html","htm",
            "json","xml",
            "mp3","mp4","webm","ogg"
    );

    /* ===================== UPLOAD ===================== */

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("folder") String folder,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity
                    .status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of(
                            "error", "File too large",
                            "message", "File size exceeds 50MB limit"
                    ));
        }

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        Path targetDir = BASE_DIR.resolve(folder).normalize();
        Path targetFile = targetDir.resolve(fileName);

        if (!targetDir.startsWith(BASE_DIR)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid path",
                    "message", "Invalid folder path"
            ));
        }

        Files.createDirectories(targetDir);

        if (Files.exists(targetFile)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "File already exists",
                    "message", "A file named '" + fileName + "' already exists in this folder."
            ));
        }

        Files.copy(file.getInputStream(), targetFile);

        return ResponseEntity.ok(Map.of(
                "fileName", fileName,
                "folder", folder,
                "viewUrl", "/api/storage/view/" + folder + "/" + fileName,
                "downloadUrl", "/api/storage/download/" + folder + "/" + fileName
        ));
    }

    /* ===================== VIEW ===================== */

    @GetMapping("/view/**")
    public ResponseEntity<?> view(HttpServletRequest request) throws IOException {
        try {
            Path filePath = resolvePath(request, "/api/storage/view/");
            if (!Files.exists(filePath)) {
                return notFound(filePath);
            }
            if (Files.isDirectory(filePath)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "Path is a directory",
                        "message", "The requested path is a directory, not a file",
                        "path", filePath.toString()
                ));
            }

            String ext = getExtension(filePath.getFileName().toString());

            if (!VIEWABLE_EXTENSIONS.contains(ext)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "File not viewable",
                        "fileName", filePath.getFileName().toString(),
                        "extension", ext,
                        "message", "This file type cannot be viewed directly. Please download it."
                ));
            }

            String mimeType = URLConnection.guessContentTypeFromName(filePath.toString());
            if (mimeType == null) {
                mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            Resource resource = new InputStreamResource(Files.newInputStream(filePath));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Access denied",
                    "message", "Invalid or unauthorized path"
            ));
        }
    }

    /* ===================== DOWNLOAD ===================== */

    @GetMapping("/download/**")
    public ResponseEntity<?> download(HttpServletRequest request) throws IOException {
        try {
            Path filePath = resolvePath(request, "/api/storage/download/");
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                throw new FileNotFoundException();
            }

            Resource resource = new InputStreamResource(Files.newInputStream(filePath));

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + filePath.getFileName() + "\"")
                    .body(resource);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "error", "Access denied",
                    "message", "Invalid or unauthorized path"
            ));
        }
    }

    /* ===================== HELPERS ===================== */

    private Path resolvePath(HttpServletRequest request, String prefix) {
        try {
            String requestURI = request.getRequestURI();
            
            // Find the prefix in the URI (handles context path like /file-api-1.0.0)
            int prefixIndex = requestURI.indexOf(prefix);
            if (prefixIndex == -1) {
                throw new SecurityException("Prefix not found in URI: " + requestURI);
            }
            
            // Extract the path after the prefix
            String path = requestURI.substring(prefixIndex + prefix.length());
            
            // Remove query parameters if any
            int queryIndex = path.indexOf('?');
            if (queryIndex != -1) {
                path = path.substring(0, queryIndex);
            }
            
            // Remove leading slash if present (Path.resolve treats leading / as absolute)
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            
            // URL decode the path to handle %20, %28, etc.
            path = URLDecoder.decode(path, StandardCharsets.UTF_8);
            
            // Resolve and normalize the path
            Path resolved = BASE_DIR.resolve(path).normalize();
            
            // Security check: ensure path stays within base directory
            if (!resolved.startsWith(BASE_DIR)) {
                throw new SecurityException("Path traversal detected: " + resolved);
            }
            
            return resolved;
        } catch (IllegalArgumentException e) {
            throw new SecurityException("Invalid path encoding: " + e.getMessage(), e);
        }
    }

    private ResponseEntity<?> notFound(String name) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "File not found",
                "message", "File '" + name + "' does not exist",
                "resolvedPath", name
        ));
    }
    
    private ResponseEntity<?> notFound(Path filePath) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "File not found",
                "message", "File does not exist at: " + filePath,
                "resolvedPath", filePath.toString()
        ));
    }

    private String getExtension(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i == -1 ? "" : fileName.substring(i + 1).toLowerCase();
    }
}

