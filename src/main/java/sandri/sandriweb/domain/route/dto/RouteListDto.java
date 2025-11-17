package sandri.sandriweb.domain.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.route.entity.Route;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteListDto {
    
    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long creatorId;
    private String creatorName;
    private String creatorNickname;
    private boolean isPublic;
    private String shareCode;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean liked;
    
    public static RouteListDto from(Route route) {
        return from(route, false);
    }

    public static RouteListDto from(Route route, boolean liked) {
        return RouteListDto.builder()
                .id(route.getId())
                .title(route.getTitle())
                .startDate(route.getStartDate())
                .endDate(route.getEndDate())
                .creatorId(route.getCreator().getId())
                .creatorName(route.getCreator().getName())
                .creatorNickname(route.getCreator().getNickname())
                .isPublic(route.isPublic())
                .shareCode(route.getShareCode())
                .imageUrl(route.getImageUrl())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .liked(liked)
                .build();
    }
}

