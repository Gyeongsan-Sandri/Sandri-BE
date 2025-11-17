package sandri.sandriweb.global.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GooglePlacesService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public GooglePlacesService(RestTemplateBuilder restTemplateBuilder,
                               @Value("${google.maps.api-key}") String apiKey,
                               @Value("${google.maps.base-url:https://maps.googleapis.com/maps/api}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    /**
     * Google Places Text Search API로 장소 검색
     * @param keyword 검색 키워드
     * @param language 언어 코드 (ko, en 등)
     * @param region 지역 코드 (kr 등)
     * @return 검색 결과 리스트
     */
    public List<PlaceSearchResult> searchPlaces(String keyword, String language, String region) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        if (!StringUtils.hasText(apiKey) || apiKey.contains("your-pop-google-maps-api-key-here")) {
            log.warn("Google Maps API Key가 설정되지 않았습니다. 빈 결과를 반환합니다.");
            return List.of();
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(baseUrl + "/place/textsearch/json")
                    .queryParam("query", keyword)
                    .queryParam("key", apiKey)
                    .queryParam("language", language != null ? language : "ko")
                    .queryParam("region", region != null ? region : "kr")
                    .build()
                    .toUri();

            log.info("Google Places API 호출: keyword={}, uri={}", keyword, uri);
            GooglePlacesResponse response = restTemplate.getForObject(uri, GooglePlacesResponse.class);

            if (response == null) {
                log.error("Google Places API 응답이 비어 있습니다.");
                return List.of();
            }

            if ("OK".equalsIgnoreCase(response.getStatus()) && response.getResults() != null) {
                return response.getResults().stream()
                        .map(PlaceSearchResult::from)
                        .collect(Collectors.toList());
            } else if ("ZERO_RESULTS".equalsIgnoreCase(response.getStatus())) {
                log.info("Google Places API 검색 결과 없음: keyword={}", keyword);
                return List.of();
            } else {
                log.error("Google Places API 오류 status={}, keyword={}", response.getStatus(), keyword);
                return List.of();
            }
        } catch (RestClientException ex) {
            log.error("Google Places API 호출 중 오류가 발생했습니다.", ex);
            return List.of();
        }
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GooglePlacesResponse {
        private String status;
        private List<GooglePlace> results;
        @JsonProperty("error_message")
        private String errorMessage;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GooglePlace {
        private String name;
        @JsonProperty("formatted_address")
        private String formattedAddress;
        private Geometry geometry;
        private Double rating;
        @JsonProperty("user_ratings_total")
        private Integer userRatingsTotal;
        private List<Photo> photos;
        private List<String> types;
        @JsonProperty("place_id")
        private String placeId;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Geometry {
        private Location location;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Location {
        private Double lat;
        private Double lng;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Photo {
        @JsonProperty("photo_reference")
        private String photoReference;
        private Integer width;
        private Integer height;
    }

    /**
     * 검색 결과 DTO
     */
    @Getter
    public static class PlaceSearchResult {
        private String placeId;
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private Double rating;
        private Integer userRatingsTotal;
        private String photoReference;
        private List<String> types;

        public static PlaceSearchResult from(GooglePlace place) {
            PlaceSearchResult result = new PlaceSearchResult();
            result.placeId = place.getPlaceId();
            result.name = place.getName();
            result.address = place.getFormattedAddress();
            
            if (place.getGeometry() != null && place.getGeometry().getLocation() != null) {
                result.latitude = place.getGeometry().getLocation().getLat();
                result.longitude = place.getGeometry().getLocation().getLng();
            }
            
            result.rating = place.getRating();
            result.userRatingsTotal = place.getUserRatingsTotal();
            
            if (place.getPhotos() != null && !place.getPhotos().isEmpty()) {
                result.photoReference = place.getPhotos().get(0).getPhotoReference();
            }
            
            result.types = place.getTypes() != null ? place.getTypes() : new ArrayList<>();
            
            return result;
        }

        /**
         * Photo Reference를 실제 이미지 URL로 변환
         * @param apiKey Google API Key
         * @param maxWidth 이미지 최대 너비 (기본 400)
         * @return 이미지 URL
         */
        public String getPhotoUrl(String apiKey, Integer maxWidth) {
            if (photoReference == null) {
                return null;
            }
            return String.format("https://maps.googleapis.com/maps/api/place/photo?maxwidth=%d&photo_reference=%s&key=%s",
                    maxWidth != null ? maxWidth : 400,
                    photoReference,
                    apiKey);
        }
    }
}

