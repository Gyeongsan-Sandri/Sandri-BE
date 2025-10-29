package sandri.sandriweb.domain.route.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import sandri.sandriweb.domain.route.dto.PlaceSearchResponseDto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleMapsService {
    
    @Value("${google.maps.api-key:}")
    private String apiKey;
    
    @Value("${google.maps.base-url:https://maps.googleapis.com/maps/api}")
    private String baseUrl;
    
    private final RestTemplate restTemplate;
    
    /**
     * Google Places API를 사용하여 장소 검색 (DTO 형태로 반환)
     * @param query 검색어 (예: "경복궁", "서울 카페")
     * @return 검색된 장소 목록
     */
    public PlaceSearchResponseDto searchPlacesDto(String query) {
        Map<String, Object> result = searchPlaces(query);
        return convertToPlaceSearchResponseDto(result);
    }
    
    /**
     * Google Places API를 사용하여 장소 검색
     * @param query 검색어 (예: "경복궁", "서울 카페")
     * @return 검색된 장소 목록 (Map 형태)
     */
    public Map<String, Object> searchPlaces(String query) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-google-maps-api-key-here")) {
            log.warn("Google Maps API 키가 설정되지 않았습니다. 더미 데이터를 반환합니다.");
            return createDummyPlaceResult(query);
        }
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/place/textsearch/json")
                    .queryParam("query", query)
                    .queryParam("key", apiKey)
                    .queryParam("language", "ko")
                    .toUriString();
            
            log.info("Google Places API 호출: {}", query);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("Google Places API 호출 실패: {}", response.getStatusCode());
                return createDummyPlaceResult(query);
            }
            
        } catch (Exception e) {
            log.error("Google Places API 호출 중 오류 발생: {}", e.getMessage(), e);
            return createDummyPlaceResult(query);
        }
    }
    
    /**
     * 장소 ID로 상세 정보 조회
     * @param placeId Google Places API의 place_id
     * @return 장소 상세 정보
     */
    public Map<String, Object> getPlaceDetails(String placeId) {
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-google-maps-api-key-here")) {
            log.warn("Google Maps API 키가 설정되지 않았습니다. 더미 데이터를 반환합니다.");
            return createDummyPlaceDetails(placeId);
        }
        
        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/place/details/json")
                    .queryParam("place_id", placeId)
                    .queryParam("key", apiKey)
                    .queryParam("language", "ko")
                    .queryParam("fields", "name,formatted_address,geometry,place_id,rating")
                    .toUriString();
            
            log.info("Google Places Details API 호출: {}", placeId);
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                log.error("Google Places Details API 호출 실패: {}", response.getStatusCode());
                return createDummyPlaceDetails(placeId);
            }
            
        } catch (Exception e) {
            log.error("Google Places Details API 호출 중 오류 발생: {}", e.getMessage(), e);
            return createDummyPlaceDetails(placeId);
        }
    }
    
    /**
     * API 키가 없을 때 사용할 더미 데이터 생성
     */
    private Map<String, Object> createDummyPlaceResult(String query) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> place = new HashMap<>();
        Map<String, Object> geometry = new HashMap<>();
        Map<String, Object> location = new HashMap<>();
        
        // 더미 위치 정보 (서울 시청 좌표)
        location.put("lat", 37.5665);
        location.put("lng", 126.9780);
        geometry.put("location", location);
        place.put("name", query + " (검색 결과 - API 키 필요)");
        place.put("formatted_address", "서울특별시 중구 세종대로 110");
        place.put("place_id", "dummy_place_id_" + System.currentTimeMillis());
        place.put("geometry", geometry);
        
        result.put("results", List.of(place));
        result.put("status", "OK");
        result.put("message", "Google Maps API 키가 설정되지 않아 더미 데이터를 반환합니다. API 키를 설정하면 실제 검색이 가능합니다.");
        
        return result;
    }
    
    private Map<String, Object> createDummyPlaceDetails(String placeId) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> place = new HashMap<>();
        Map<String, Object> geometry = new HashMap<>();
        Map<String, Object> location = new HashMap<>();
        
        location.put("lat", 37.5665);
        location.put("lng", 126.9780);
        geometry.put("location", location);
        place.put("name", "더미 장소 (API 키 필요)");
        place.put("formatted_address", "서울특별시 중구 세종대로 110");
        place.put("place_id", placeId);
        place.put("geometry", geometry);
        
        result.put("result", place);
        result.put("status", "OK");
        result.put("message", "Google Maps API 키가 설정되지 않아 더미 데이터를 반환합니다.");
        
        return result;
    }
    
    /**
     * Map 형태의 결과를 DTO로 변환
     */
    private PlaceSearchResponseDto convertToPlaceSearchResponseDto(Map<String, Object> result) {
        String status = (String) result.getOrDefault("status", "ERROR");
        String message = (String) result.getOrDefault("message", "");
        
        List<PlaceSearchResponseDto.PlaceDto> places = new ArrayList<>();
        
        if (result.containsKey("results")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) result.get("results");
            
            for (Map<String, Object> placeMap : results) {
                PlaceSearchResponseDto.PlaceDto place = convertToPlaceDto(placeMap);
                places.add(place);
            }
        }
        
        return PlaceSearchResponseDto.builder()
                .status(status)
                .message(message)
                .results(places)
                .build();
    }
    
    private PlaceSearchResponseDto.PlaceDto convertToPlaceDto(Map<String, Object> placeMap) {
        String placeId = (String) placeMap.get("place_id");
        String name = (String) placeMap.get("name");
        String formattedAddress = (String) placeMap.get("formatted_address");
        
        Double lat = null;
        Double lng = null;
        Double rating = placeMap.get("rating") != null ? ((Number) placeMap.get("rating")).doubleValue() : null;
        
        if (placeMap.containsKey("geometry")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> geometry = (Map<String, Object>) placeMap.get("geometry");
            if (geometry.containsKey("location")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                if (location.get("lat") != null) {
                    lat = ((Number) location.get("lat")).doubleValue();
                }
                if (location.get("lng") != null) {
                    lng = ((Number) location.get("lng")).doubleValue();
                }
            }
        }
        
        PlaceSearchResponseDto.PlaceDto.LocationDto locationDto = PlaceSearchResponseDto.PlaceDto.LocationDto.builder()
                .lat(lat)
                .lng(lng)
                .build();
        
        return PlaceSearchResponseDto.PlaceDto.builder()
                .placeId(placeId)
                .name(name)
                .formattedAddress(formattedAddress)
                .location(locationDto)
                .rating(rating)
                .build();
    }
}

