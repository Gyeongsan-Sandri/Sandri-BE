package sandri.sandriweb.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AWS S3 파일 업로드 서비스 구현체
 * TODO: AWS S3 SDK 설정 후 실제 구현 필요
 * - AWS SDK 의존성 추가 (implementation 'software.amazon.awssdk:s3')
 * - application.yml에 AWS 설정 추가
 * - 실제 S3 업로드 로직 구현
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3ServiceImpl implements S3Service {

    @Override
    public String uploadFile(MultipartFile file) {
        // TODO: AWS S3 SDK를 사용한 실제 업로드 구현
        log.warn("AWS S3 업로드가 아직 구현되지 않았습니다. 파일명: {}", file.getOriginalFilename());
        
        // 임시로 로컬 파일명 반환 (실제 구현 시 S3 URL 반환)
        // return "https://your-s3-bucket.s3.region.amazonaws.com/" + file.getOriginalFilename();
        
        throw new UnsupportedOperationException("AWS S3 업로드 기능이 아직 구현되지 않았습니다. AWS SDK 설정이 필요합니다.");
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files) {
        return files.stream()
                .map(this::uploadFile)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteFile(String fileUrl) {
        // TODO: AWS S3 SDK를 사용한 실제 삭제 구현
        log.warn("AWS S3 파일 삭제가 아직 구현되지 않았습니다. URL: {}", fileUrl);
        throw new UnsupportedOperationException("AWS S3 파일 삭제 기능이 아직 구현되지 않았습니다.");
    }
}

