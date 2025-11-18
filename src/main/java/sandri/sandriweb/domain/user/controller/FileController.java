package sandri.sandriweb.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sandri.sandriweb.domain.review.dto.GetPresignedUrlsResponseDto;
import sandri.sandriweb.domain.review.dto.PresignedUrlDto;
import sandri.sandriweb.domain.review.dto.RequestPresignedUrlRequestDto;
import sandri.sandriweb.domain.user.dto.ApiResponseDto;
import sandri.sandriweb.global.service.S3Service;

import java.util.List;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "파일", description = "파일 업로드 관련 API")
public class FileController {
    
    private final S3Service s3Service;
    
    @PostMapping("/files")
    @Operation(summary = "Presigned URL 발급 (사진/영상 업로드용)", 
               description = "사진/영상 업로드가 필요할 때 업로드 전 호출합니다. " +
                             "프론트엔드에서 선택한 파일들의 파일명, order, Content-Type을 전송하면, " +
                             "각 파일에 대한 Presigned URL을 발급하고 이와 함께 finalUrl과 order를 반환합니다. " +
                             "발급된 Presigned URL로 PUT 요청을 보내 파일을 업로드하고, " +
                             "업로드 완료 후 finalUrl과 order를 리뷰 작성 시 사용합니다. " +
                             "인증 없이 사용 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Presigned URL 발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponseDto<GetPresignedUrlsResponseDto>> getPresignedUrls(
            @Valid @RequestBody RequestPresignedUrlRequestDto request) {

        log.info("Presigned URL 발급 요청: fileCount={}", 
                 request.getFiles() != null ? request.getFiles().size() : 0);

        try {
            // 파일 정보 리스트 검증 (@Valid로 자동 검증되지만 추가 안전장치)
            if (request.getFiles() == null || request.getFiles().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponseDto.error("파일 정보 리스트가 필요합니다."));
            }

            // 파일 타입 검증 (이미지 및 비디오만 허용)
            // 이미지: image/jpeg, image/png, image/gif, image/webp, image/bmp 등
            // 비디오: video/mp4, video/quicktime, video/x-msvideo 등
            for (RequestPresignedUrlRequestDto.FileInfo fileInfo : request.getFiles()) {
                String contentType = fileInfo.getContentType();
                if (contentType == null || contentType.isBlank()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponseDto.error("파일 타입(Content-Type)이 필요합니다."));
                }
                
                // MIME 타입은 image/* 또는 video/* 형식으로 시작
                if (!contentType.startsWith("image/") && !contentType.startsWith("video/")) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponseDto.error("지원하지 않는 파일 타입입니다. 이미지 또는 비디오 파일만 업로드 가능합니다. (현재: " + contentType + ")"));
                }
            }

            // Presigned URL 생성 (S3Service에서 고유한 파일명으로 변환, order 포함)
            List<PresignedUrlDto> presignedUrls = request.getFiles().stream()
                    .map(fileInfo -> {
                        log.debug("Presigned URL 생성: fileName={}, contentType={}, order={}", 
                                 fileInfo.getFileName(), fileInfo.getContentType(), fileInfo.getOrder());
                        PresignedUrlDto presignedUrlDto = s3Service.generatePresignedUrl(fileInfo.getFileName(), fileInfo.getContentType());
                        // order를 포함하여 반환
                        return PresignedUrlDto.builder()
                                .fileName(presignedUrlDto.getFileName())
                                .presignedUrl(presignedUrlDto.getPresignedUrl())
                                .finalUrl(presignedUrlDto.getFinalUrl())
                                .order(fileInfo.getOrder())
                                .build();
                    })
                    .collect(java.util.stream.Collectors.toList());

            GetPresignedUrlsResponseDto response = GetPresignedUrlsResponseDto.builder()
                    .presignedUrls(presignedUrls)
                    .build();

            log.info("Presigned URL 발급 완료: fileCount={}", presignedUrls.size());
            return ResponseEntity.ok(ApiResponseDto.success(response));
        } catch (Exception e) {
            log.error("Presigned URL 생성 중 오류 발생: ", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponseDto.error("Presigned URL 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

