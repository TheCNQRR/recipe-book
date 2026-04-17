package by.java.enterprise.recipe.book.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final S3Client s3Client;

    @Value("${app.storage.s3.bucket}")
    private String bucket;

    @Value("${app.storage.s3.public-url-prefix:#{null}}")
    private String publicUrlPrefix;

    public String uploadFile(MultipartFile file) {
        String filename = generateUniqueFilename(file.getOriginalFilename());
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(filename)
                    .contentType(file.getContentType())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

            return buildPublicUrl(filename);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке файла в S3", e);
        }
    }

    private String generateUniqueFilename(String originalName) {
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }
        return UUID.randomUUID() + ext;
    }

    private String buildPublicUrl(String key) {
        if (publicUrlPrefix != null && !publicUrlPrefix.isBlank()) {
            return publicUrlPrefix + "/" + key;
        }
        return String.format("https://%s.s3.%s.amazonaws.com/%s",
                bucket, s3Client.serviceClientConfiguration().region().id(), key);
    }
}
