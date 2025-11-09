package sandri.sandriweb.global.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sandri.sandriweb.domain.review.dto.PresignedUrlDto;

import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AWS S3 파일 업로드 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImpl implements S3Service {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.region.static}")
    private String region;

    private static final int PRESIGNED_URL_EXPIRY_MINUTES = 5; // Presigned URL 만료 시간 (5분)

    /**
     * BASE_URL 생성 (버킷 이름과 리전 기반)
     */
    private String getBaseUrl() {
        return String.format("https://%s.s3.%s.amazonaws.com", bucketName, region);
    }

    @Override
    public String uploadFile(MultipartFile file) {
        // TODO: AWS S3 SDK를 사용한 실제 업로드 구현
        log.warn("AWS S3 업로드가 아직 구현되지 않았습니다. 파일명: {}", file.getOriginalFilename());
        throw new UnsupportedOperationException("AWS S3 업로드 기능이 아직 구현되지 않았습니다. AWS SDK 설정이 필요합니다.");
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files) {
        return files.stream()
                .map(this::uploadFile)
                .collect(Collectors.toList());
    }

    @Override
    public PresignedUrlDto generatePresignedUrl(String fileName, String contentType) {
        // 고유한 파일명 생성
        String uniqueFileName = generateUniqueFileName(fileName);
        
        // 만료 시간 설정 (현재 시간 + 5분)
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000L * 60 * PRESIGNED_URL_EXPIRY_MINUTES;
        expiration.setTime(expTimeMillis);
        
        // Presigned URL 생성 요청
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(
                bucketName,
                uniqueFileName
        )
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration)
                .withContentType(contentType);
        
        // Presigned URL 생성
        URL presignedUrl = amazonS3Client.generatePresignedUrl(generatePresignedUrlRequest);
        
        // 최종 URL 생성
        String finalUrl = getBaseUrl() + "/" + uniqueFileName;
        
        log.debug("Presigned URL 생성 완료: fileName={}, contentType={}", uniqueFileName, contentType);
        
        return PresignedUrlDto.builder()
                .fileName(uniqueFileName)
                .presignedUrl(presignedUrl.toString())
                .finalUrl(finalUrl)
                .build();
    }

    @Override
    public void deleteFile(String fileUrl) {
        // TODO: AWS S3 SDK를 사용한 실제 삭제 구현
        log.warn("AWS S3 파일 삭제가 아직 구현되지 않았습니다. URL: {}", fileUrl);
        throw new UnsupportedOperationException("AWS S3 파일 삭제 기능이 아직 구현되지 않았습니다.");
    }

    /**
     * 고유한 파일명 생성 (UUID + 원본 파일명)
     */
    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFileName.substring(lastDotIndex);
            originalFileName = originalFileName.substring(0, lastDotIndex);
        }
        
        String timestamp = String.valueOf(Instant.now().toEpochMilli());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        
        return String.format("reviews/%s_%s_%s%s", timestamp, uuid, originalFileName, extension);
    }
}

