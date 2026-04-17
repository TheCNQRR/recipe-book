package by.java.enterprise.recipe.book.controller;

import by.java.enterprise.recipe.book.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = uploadSingle(file);
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/images")
    public ResponseEntity<?> uploadImages(@RequestParam("files") List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Не выбраны файлы"));
        }
        if (files.size() > 5) {
            return ResponseEntity.badRequest().body(Map.of("error", "Максимум 5 файлов"));
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadSingle(file));
        }
        return ResponseEntity.ok(Map.of("urls", urls));
    }

    private String uploadSingle(MultipartFile file) {
        if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Файл пуст или превышает 5 МБ");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Разрешены только изображения");
        }
        return storageService.uploadFile(file);
    }
}
