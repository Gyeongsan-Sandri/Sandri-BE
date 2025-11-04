package sandri.sandriweb.domain.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestPresignedUrlRequestDto {
    
    @NotNull(message = "파일 정보는 필수입니다")
    private List<FileInfo> files;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileInfo {
        @NotBlank(message = "파일명은 필수입니다")
        private String fileName;
        
        @NotBlank(message = "파일 타입(Content-Type)은 필수입니다")
        private String contentType;  // image/jpeg, image/png, video/mp4 등
    }
}

