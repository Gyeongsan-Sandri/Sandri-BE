package sandri.sandriweb.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacePhotoCursorResponseDto {
    private List<PlacePhotoDto> photos;
    private int size;
    private Long nextCursor;
    private boolean hasNext;
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlacePhotoDto {
        private Long placeId;
        private String photoUrl;
    }
}

