package sandri.sandriweb.domain.dataImport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import sandri.sandriweb.domain.dataImport.dto.GooglePlaceDetailsResponse;
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

    @Value("${google.places.find-place-url}")
    private String findPlaceUrl;

    @Value("${google.places.details-url}")
    private String detailsUrl;

    @Value("${google.places.photo-url}")
    private String photoUrl;

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
                    .fromHttpUrl(findPlaceUrl)
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
     * Google Place Details API (New)로 장소 상세 정보 조회
     * @param placeId Place ID
     * @return Place Details 응답 (photos 최대 10장, editorialSummary 포함)
     */
    public GooglePlaceDetailsResponse getPlaceDetails(String placeId) {
        try {
            String url = detailsUrl + "/" + placeId;

            log.info("Google Place Details API 호출: placeId={}", placeId);
            log.info("Google Place Details API URL: {}", url);

            // New Places API는 헤더로 인증 및 필드 지정
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Goog-Api-Key", googleApiKey);
            headers.set("X-Goog-FieldMask", "id,displayName,formattedAddress,location,photos,editorialSummary");

            // 쿼리 파라미터로 언어 및 지역 설정
            String urlWithParams = UriComponentsBuilder
                    .fromHttpUrl(url)
                    .queryParam("languageCode", "ko")
                    .queryParam("regionCode", "KR")
                    .build()
                    .toUriString();

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<String> response =
                    restTemplate.exchange(urlWithParams, org.springframework.http.HttpMethod.GET, entity, String.class);

            log.info("Google Place Details API 응답: {}", response.getBody());

            GooglePlaceDetailsResponse detailsResponse = objectMapper.readValue(response.getBody(), GooglePlaceDetailsResponse.class);

            if (detailsResponse != null && detailsResponse.getId() != null) {
                log.info("Google Place Details API 조회 성공: placeId={}", placeId);
                return detailsResponse;
            } else {
                log.warn("Google Place Details API 조회 실패: placeId={}", placeId);
                return null;
            }

        } catch (Exception e) {
            log.error("Google Place Details API 호출 실패: placeId={}, error={}", placeId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Google Place API의 photo reference를 사용하여 사진 URL 생성 (Old API)
     * @param photoReference 사진 참조 ID
     * @param maxWidth 최대 너비 (픽셀)
     * @return 사진 URL
     */
    public String getPhotoUrl(String photoReference, int maxWidth) {
        return UriComponentsBuilder
                .fromHttpUrl(photoUrl)
                .queryParam("maxwidth", maxWidth)
                .queryParam("photo_reference", photoReference)
                .queryParam("key", googleApiKey)
                .build()
                .toUriString();
    }

    /**
     * New Places API의 photo name을 사용하여 사진 URL 생성
     * @param photoName places/{PLACE_ID}/photos/{PHOTO_REF} 형식
     * @param maxWidthPx 최대 너비 (픽셀)
     * @return 사진 URL
     */
    public String getPhotoUrlFromName(String photoName, int maxWidthPx) {
        // New Places API photo URL: https://places.googleapis.com/v1/{photoName}/media
        String url = "https://places.googleapis.com/v1/" + photoName + "/media";

        return UriComponentsBuilder
                .fromHttpUrl(url)
                .queryParam("maxWidthPx", maxWidthPx)
                .queryParam("key", googleApiKey)
                .build()
                .toUriString();
    }
}
