package sandri.sandriweb.global.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * AWS S3 파일 업로드 서비스 인터페이스
 * TODO: AWS S3 SDK 설정 후 구현 필요
 */
public interface S3Service {
    /**
     * 단일 파일을 S3에 업로드
     * @param file 업로드할 파일
     * @return 업로드된 파일의 URL
     */
    String uploadFile(MultipartFile file);

    /**
     * 여러 파일을 S3에 업로드
     * @param files 업로드할 파일 리스트
     * @return 업로드된 파일들의 URL 리스트
     */
    List<String> uploadFiles(List<MultipartFile> files);

    /**
     * S3에서 파일 삭제
     * @param fileUrl 삭제할 파일의 URL
     */
    void deleteFile(String fileUrl);
}

