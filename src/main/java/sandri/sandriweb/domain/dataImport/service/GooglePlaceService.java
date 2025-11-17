package sandri.sandriweb.domain.dataImport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import sandri.sandriweb.domain.dataImport.dto.GooglePlaceResponse;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class GooglePlaceService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.maps.api-key}")
    private String googleApiKey;

    private static final String GOOGLE_PLACE_API_URL = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json";

    /**
     * Google Place API를 사용하여 장소 정보 검색
     * @param placeName 검색할 장소명
     * @param address 주소 (검색 정확도 향상용)
     * @return Google Place API 응답
     */
    public GooglePlaceResponse findPlace(String placeName, String address) {
        try {
            // 검색 쿼리 생성: "장소명 주소" 형태로 검색 정확도 향상
            String query = placeName;
            if (address != null && !address.isEmpty()) {
                query = placeName + " " + address;
            }

            String url = UriComponentsBuilder
                    .fromHttpUrl(GOOGLE_PLACE_API_URL)
                    .queryParam("input", query)
                    .queryParam("inputtype", "textquery")
                    .queryParam("fields", "formatted_address,name,geometry,place_id,photo")
                    .queryParam("key", googleApiKey)
                    .build()
                    .toUriString();

            log.info("Google Place API 호출: query={}", query);
            log.info("Google Place API URL: {}", url);

            String response = restTemplate.getForObject(url, String.class);
            log.info("Google Place API 응답: {}", response);

            GooglePlaceResponse placeResponse = objectMapper.readValue(response, GooglePlaceResponse.class);

            if ("OK".equals(placeResponse.getStatus()) && placeResponse.getCandidates() != null && !placeResponse.getCandidates().isEmpty()) {
                log.info("Google Place API 검색 성공: placeName={}", placeName);
                return placeResponse;
            } else {
                log.warn("Google Place API 검색 결과 없음: placeName={}, status={}", placeName, placeResponse.getStatus());
                return null;
            }

        } catch (Exception e) {
            log.error("Google Place API 호출 실패: placeName={}, error={}", placeName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Google Place API의 photo reference를 사용하여 사진 URL 생성
     * @param photoReference 사진 참조 ID
     * @param maxWidth 최대 너비 (픽셀)
     * @return 사진 URL
     */
    public String getPhotoUrl(String photoReference, int maxWidth) {
        return UriComponentsBuilder
                .fromHttpUrl("https://maps.googleapis.com/maps/api/place/photo")
                .queryParam("maxwidth", maxWidth)
                .queryParam("photo_reference", photoReference)
                .queryParam("key", googleApiKey)
                .build()
                .toUriString();
    }
}
