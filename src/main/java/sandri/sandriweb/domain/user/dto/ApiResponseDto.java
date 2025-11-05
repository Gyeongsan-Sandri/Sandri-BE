package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공통 API 응답 DTO")
public class ApiResponseDto<T> {
    
    @Schema(description = "성공 여부", example = "true")
    private boolean success;
    
    @Schema(description = "응답 메시지", example = "성공")
    private String message;
    
    @Schema(description = "응답 데이터")
    private T data;
    
    public static <T> ApiResponseDto<T> success(T data) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message("성공")
                .data(data)
                .build();
    }
    
    public static <T> ApiResponseDto<T> success(String message, T data) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .build();
    }
    
    public static <T> ApiResponseDto<T> error(String message) {
        return ApiResponseDto.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .build();
    }
}
