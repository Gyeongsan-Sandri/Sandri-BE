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
import sandri.sandriweb.global.service.dto.GeocodingResult;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GoogleGeocodingService {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public GoogleGeocodingService(RestTemplateBuilder restTemplateBuilder,
                                  @Value("${google.maps.api-key}") String apiKey,
                                  @Value("${google.maps.base-url:https://maps.googleapis.com/maps/api}") String baseUrl) {
        this.restTemplate = restTemplateBuilder.build();
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    /**
     * 주소를 위도/경도로 변환
     * @param address 한글 주소
     * @return 좌표 정보 (Optional)
     */
    public Optional<GeocodingResult> geocode(String address) {
        if (!StringUtils.hasText(address)) {
            return Optional.empty();
        }

        if (!StringUtils.hasText(apiKey) || apiKey.contains("your-pop-google-maps-api-key-here")) {
            log.error("Google Maps API Key가 설정되지 않았습니다.");
            throw new IllegalStateException("Google Maps API Key가 필요합니다. 환경변수를 확인하세요.");
        }

        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(baseUrl + "/geocode/json")
                    .queryParam("address", address)
                    .queryParam("key", apiKey)
                    .build(true)
                    .toUri();

            GoogleGeocodingResponse response = restTemplate.getForObject(uri, GoogleGeocodingResponse.class);

            if (response == null) {
                log.error("Google Geocoding API 응답이 비어 있습니다.");
                return Optional.empty();
            }

            if ("OK".equalsIgnoreCase(response.getStatus())
                    && response.getResults() != null
                    && !response.getResults().isEmpty()) {
                GoogleGeocodingResponse.Result firstResult = response.getResults().get(0);
                if (firstResult.getGeometry() != null && firstResult.getGeometry().getLocation() != null) {
                    return Optional.of(new GeocodingResult(
                            firstResult.getGeometry().getLocation().getLat(),
                            firstResult.getGeometry().getLocation().getLng(),
                            firstResult.getFormattedAddress()
                    ));
                }
            } else if ("ZERO_RESULTS".equalsIgnoreCase(response.getStatus())) {
                log.warn("Google Geocoding API 결과가 없습니다. address={}", address);
                return Optional.empty();
            } else {
                log.error("Google Geocoding API 오류 status={}, address={}", response.getStatus(), address);
                throw new RuntimeException("Google Geocoding API 호출 실패: " + response.getStatus());
            }
        } catch (RestClientException ex) {
            log.error("Google Geocoding API 호출 중 오류가 발생했습니다.", ex);
            throw new RuntimeException("주소 좌표 조회 중 오류가 발생했습니다.");
        }

        return Optional.empty();
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoogleGeocodingResponse {
        private String status;
        private List<Result> results;

        @Getter
        @JsonIgnoreProperties(ignoreUnknown = true)
        private static class Result {
            private Geometry geometry;
            @JsonProperty("formatted_address")
            private String formattedAddress;
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
    }
}

