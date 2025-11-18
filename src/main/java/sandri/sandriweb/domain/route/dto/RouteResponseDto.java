package sandri.sandriweb.domain.route.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.route.entity.Route;
import sandri.sandriweb.domain.route.entity.RouteLocation;
import sandri.sandriweb.domain.route.entity.RouteParticipant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteResponseDto {
    
    private Long id;
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long creatorId;
    private String creatorName;
    private String creatorNickname;
    @JsonProperty("public")
    private boolean isPublic;
    private String shareCode;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ParticipantDto> participants;
    private List<LocationDto> locations;
    
    public static RouteResponseDto from(Route route) {
        return RouteResponseDto.builder()
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
                .participants(route.getParticipants().stream()
                        .map(ParticipantDto::from)
                        .collect(Collectors.toList()))
                .locations(route.getLocations().stream()
                        .map(LocationDto::from)
                        .collect(Collectors.toList()))
                .build();
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDto {
        private Long id;
        private Long userId;
        private String userName;
        private String userNickname;
        private LocalDateTime joinedAt;
        
        public static ParticipantDto from(RouteParticipant participant) {
            return ParticipantDto.builder()
                    .id(participant.getId())
                    .userId(participant.getUser().getId())
                    .userName(participant.getUser().getName())
                    .userNickname(participant.getUser().getNickname())
                    .joinedAt(participant.getJoinedAt())
                    .build();
        }
    }
    
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationDto {
        private Long id;
        private Integer dayNumber;
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private String description;
        private Integer displayOrder;
        private String memo;
        
        public static LocationDto from(RouteLocation location) {
            return LocationDto.builder()
                    .id(location.getId())
                    .dayNumber(location.getDayNumber())
                    .name(location.getName())
                    .address(location.getAddress())
                    .latitude(location.getLatitude() != null ? location.getLatitude().doubleValue() : null)
                    .longitude(location.getLongitude() != null ? location.getLongitude().doubleValue() : null)
                    .description(location.getDescription())
                    .displayOrder(location.getDisplayOrder())
                    .memo(location.getMemo())
                    .build();
        }
    }
}

