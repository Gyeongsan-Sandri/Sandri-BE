package sandri.sandriweb.domain.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    
    @NotNull(message = "파일 정보 리스트는 필수입니다")
    @Size(min = 1, max = 10, message = "최소 1개, 최대 10개까지 가능합니다")
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

