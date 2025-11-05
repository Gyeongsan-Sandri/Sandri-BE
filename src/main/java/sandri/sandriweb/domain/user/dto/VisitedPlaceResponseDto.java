package sandri.sandriweb.domain.user.dto;

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
public class VisitedPlaceResponseDto {

    private Long userVisitedPlaceId;
    private Long placeId;
    private String placeName;
    private String placeAddress;
    private String placeThumbnailUrl;
    private LocalDate visitDate;
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

