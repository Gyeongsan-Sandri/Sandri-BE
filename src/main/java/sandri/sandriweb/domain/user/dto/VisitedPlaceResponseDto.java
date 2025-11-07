package sandri.sandriweb.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.user.entity.UserVisitedPlace;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "방문 장소 응답 DTO")
public class VisitedPlaceResponseDto {

    @Schema(description = "방문 기록 ID", example = "1")
    private Long userVisitedPlaceId;
    
    @Schema(description = "장소 ID", example = "1")
    private Long placeId;
    
    @Schema(description = "장소 이름", example = "경주 불국사")
    private String placeName;
    
    @Schema(description = "장소 주소", example = "경상북도 경주시 불국로 385")
    private String placeAddress;
    
    @Schema(description = "장소 썸네일 URL", example = "https://s3.../photo.jpg")
    private String placeThumbnailUrl;
    
    @Schema(description = "방문 날짜", example = "2024-11-05")
    private LocalDate visitDate;
    
    @Schema(description = "리뷰 작성 여부", example = "true")
    private boolean hasReview;

    public static VisitedPlaceResponseDto from(UserVisitedPlace userVisitedPlace, String thumbnailUrl, boolean hasReview) {
        Place place = userVisitedPlace.getPlace();

        return VisitedPlaceResponseDto.builder()
                .userVisitedPlaceId(userVisitedPlace.getId())
                .placeId(place.getId())
                .placeName(place.getName())
                .placeAddress(place.getAddress())
                .placeThumbnailUrl(thumbnailUrl)
                .visitDate(userVisitedPlace.getVisitDate())
                .hasReview(hasReview)
                .build();
    }
}

