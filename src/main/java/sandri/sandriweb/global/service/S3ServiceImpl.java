package sandri.sandriweb.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import sandri.sandriweb.domain.review.dto.PresignedUrlDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AWS S3 파일 업로드 서비스 구현체
 * TODO: AWS S3 SDK 설정 후 실제 구현 필요
 * - AWS SDK 의존성 추가 (implementation 'software.amazon.awssdk:s3')
 * - application.yml에 AWS 설정 추가
 * - 실제 S3 업로드 및 Presigned URL 생성 로직 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImpl implements S3Service {

    // TODO: 실제 S3 설정 값으로 변경
    private static final String BUCKET_NAME = "your-bucket-name";
    private static final String REGION = "ap-northeast-2";
    private static final String BASE_URL = String.format("https://%s.s3.%s.amazonaws.com", BUCKET_NAME, REGION);
    private static final int PRESIGNED_URL_EXPIRY_MINUTES = 5; // Presigned URL 만료 시간 (5분)

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
        // TODO: AWS S3 SDK를 사용한 실제 Presigned URL 생성 구현
        // 예시 코드:
        // S3Presigner presigner = S3Presigner.create();
        // PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
        //     .signatureDuration(Duration.ofMinutes(PRESIGNED_URL_EXPIRY_MINUTES))
        //     .putObjectRequest(poRequest -> poRequest
        //         .bucket(BUCKET_NAME)
        //         .key(fileName)
        //         .contentType(contentType))
        //     .build();
        // PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        // String presignedUrl = presignedRequest.url().toString();
        // String finalUrl = BASE_URL + "/" + fileName;
        
        log.warn("Presigned URL 생성이 아직 구현되지 않았습니다. 파일명: {}, 타입: {}", fileName, contentType);
        
        // 임시 구현 (실제 구현 시 위 주석 코드 사용)
        String uniqueFileName = generateUniqueFileName(fileName);
        String presignedUrl = BASE_URL + "/" + uniqueFileName + "?presigned=true"; // 임시 URL
        String finalUrl = BASE_URL + "/" + uniqueFileName;
        
        return PresignedUrlDto.builder()
                .fileName(uniqueFileName)
                .presignedUrl(presignedUrl)
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

