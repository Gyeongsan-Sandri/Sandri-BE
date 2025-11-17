package sandri.sandriweb.domain.dataImport.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import sandri.sandriweb.domain.dataImport.dto.GbgsTourApiResponse;
import sandri.sandriweb.domain.dataImport.dto.GooglePlaceResponse;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.entity.PlacePhoto;
import sandri.sandriweb.domain.place.enums.Category;
import sandri.sandriweb.domain.place.enums.PlaceCategory;
import sandri.sandriweb.domain.place.repository.PlaceRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataImportService {

    private final PlaceRepository placeRepository;
    private final GooglePlaceService googlePlaceService;
    private final EntityManager entityManager;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${external.api.service-key}")
    private String serviceKey;

    @Value("${external.api.base-url:http://apis.data.go.kr/5130000/openapi/GbgsTourService/getTourContentList}")
    private String externalApiBaseUrl;

    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final int[] CATEGORY_CODES = {100, 200, 300, 400}; // 음식, 숙박, 관광명소, 문화재/역사
    private static final int NUM_OF_ROWS = 100; // 한 페이지당 조회 개수

    /**
     * 외부 API에서 데이터를 가져와 Google Place API로 검색 후 DB에 저장
     * @return 처리 결과 메시지
     */
    @Transactional
    public String importPlacesFromExternalApi() {
        int totalImported = 0;
        int totalFailed = 0;
        int totalSkipped = 0;

        log.info("데이터 임포트 시작");

        try {
            // 공공 API 호출 가능 여부 확인 (첫 번째 카테고리로 테스트)
            int firstCode = CATEGORY_CODES[0];
            GbgsTourApiResponse testResponse = fetchExternalApiData(firstCode, 1, NUM_OF_ROWS);
            
            if (testResponse == null) {
                String errorMsg = String.format("공공 API 호출 실패: 카테고리 코드 %d의 첫 페이지 조회에 실패했습니다. " +
                        "API 서버 상태, 네트워크 연결, API 키를 확인해주세요.", firstCode);
                log.error(errorMsg);
                throw new RuntimeException(errorMsg);
            }

            // 각 카테고리 코드별로 순회
            for (int code : CATEGORY_CODES) {
                log.info("카테고리 코드 {} 처리 시작", code);

                // 1단계: 첫 페이지를 조회하여 totalCount 확인
                GbgsTourApiResponse firstPageResponse;
                
                // 첫 번째 카테고리는 이미 조회했으므로 재사용
                if (code == firstCode) {
                    firstPageResponse = testResponse;
                } else {
                    firstPageResponse = fetchExternalApiData(code, 1, NUM_OF_ROWS);
                }

                if (firstPageResponse == null || firstPageResponse.getTotalCount() == null) {
                    log.warn("카테고리 코드 {} 첫 페이지 조회 실패", code);
                    continue;
                }

                int totalCount = firstPageResponse.getTotalCount();
                log.info("카테고리 코드 {}: 총 {} 개의 데이터 발견", code, totalCount);

                // 2단계: 전체 페이지 수 계산
                int totalPages = (int) Math.ceil((double) totalCount / NUM_OF_ROWS);

                // 3단계: 모든 페이지 순회
                for (int pageNo = 1; pageNo <= totalPages; pageNo++) {
                    GbgsTourApiResponse response;

                    // 첫 페이지는 이미 조회했으므로 재사용
                    if (pageNo == 1) {
                        response = firstPageResponse;
                    } else {
                        response = fetchExternalApiData(code, pageNo, NUM_OF_ROWS);
                    }

                    if (response == null || response.getItem() == null || response.getItem().isEmpty()) {
                        log.warn("카테고리 코드 {}, 페이지 {} 데이터 없음", code, pageNo);
                        continue;
                    }

                    List<GbgsTourApiResponse.TourItem> items = response.getItem();
                    log.info("카테고리 코드 {}, 페이지 {}/{}: {} 개 항목 처리", code, pageNo, totalPages, items.size());

                    // 4단계: 각 항목 처리
                    for (GbgsTourApiResponse.TourItem item : items) {
                        try {
                            // 외부 API 이름으로 먼저 중복 체크 (Google API 호출 전에)
                            if (placeRepository.existsByName(item.getTitle())) {
                                log.debug("이미 존재하는 장소 (외부 API 이름): {}", item.getTitle());
                                totalSkipped++;
                                continue;
                            }

                            // Google Place API로 검색
                            GooglePlaceResponse googlePlace = googlePlaceService.findPlace(
                                item.getTitle(),
                                item.getAddress()
                            );

                            if (googlePlace != null && googlePlace.getCandidates() != null
                                && !googlePlace.getCandidates().isEmpty()) {

                                GooglePlaceResponse.Candidate candidate = googlePlace.getCandidates().get(0);

                                // Place 엔티티 생성 및 저장
                                Place savedPlace = createAndSavePlace(item, candidate, code);

                                if (savedPlace != null) {
                                    totalImported++;
                                    log.info("장소 저장 성공: {}", item.getTitle());
                                } else {
                                    totalFailed++;
                                    // 저장 실패 시 세션 클리어하여 다음 아이템 처리 가능하도록 함
                                    entityManager.clear();
                                    log.info("세션 클리어 완료 (저장 실패 후)");
                                }
                            } else {
                                log.warn("Google Place API에서 찾을 수 없음: {}", item.getTitle());
                                totalFailed++;
                            }

                        } catch (Exception e) {
                            log.error("장소 처리 중 오류: name={}, error={}", item.getTitle(), e.getMessage(), e);
                            totalFailed++;
                            // 예외 발생 시 세션 클리어하여 다음 아이템 처리 가능하도록 함
                            entityManager.clear();
                            log.info("세션 클리어 완료 (예외 발생 후)");
                        }
                    }
                }
            }

            String result = String.format("데이터 임포트 완료 - 성공: %d, 실패: %d, 스킵: %d",
                totalImported, totalFailed, totalSkipped);
            log.info(result);
            return result;

        } catch (Exception e) {
            log.error("데이터 임포트 중 오류 발생", e);
            throw new RuntimeException("데이터 임포트 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 외부 API 호출
     */
    private GbgsTourApiResponse fetchExternalApiData(int code, int pageNo, int numOfRows) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(externalApiBaseUrl)
                    .queryParam("serviceKey", serviceKey)
                    .queryParam("pageNo", pageNo)
                    .queryParam("numOfRows", numOfRows)
                    .queryParam("code", code)
                    .queryParam("returnType", "json")
                    .build(false)
                    .toUriString();

            log.info("외부 API 호출: code={}, pageNo={}, numOfRows={}", code, pageNo, numOfRows);
            log.info("외부 API URL: {}", url);

            String responseStr = restTemplate.getForObject(url, String.class);
            log.info("외부 API 응답: {}", responseStr);

            return objectMapper.readValue(responseStr, GbgsTourApiResponse.class);

        } catch (Exception e) {
            log.error("외부 API 호출 실패: code={}, pageNo={}, error={}", code, pageNo, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Place 엔티티 생성 및 저장 (Google Place API 데이터 우선 사용)
     */
    private Place createAndSavePlace(GbgsTourApiResponse.TourItem item,
                                     GooglePlaceResponse.Candidate googleCandidate,
                                     int categoryCode) {
        try {
            // 좌표 생성 (Google Place API 기준)
            Point location = geometryFactory.createPoint(
                new Coordinate(
                    googleCandidate.getGeometry().getLocation().getLng(),
                    googleCandidate.getGeometry().getLocation().getLat()
                )
            );

            // 카테고리 매핑
            PlaceCategory group = mapCodeToPlaceCategory(categoryCode);
            Category category = mapCodeToCategory(categoryCode);

            // 장소명 (Google 우선, 없으면 외부 API 사용)
            String name = googleCandidate.getName();
            if (name == null || name.isEmpty()) {
                name = item.getTitle();
            }

            // 주소 (Google 우선, 없으면 외부 API 사용)
            String address = googleCandidate.getFormattedAddress();
            if (address == null || address.isEmpty()) {
                address = item.getAddress();
            }

            // 요약 정보 (Google의 editorial_summary 우선)
            String summary = null;
            if (googleCandidate.getEditorialSummary() != null) {
                summary = googleCandidate.getEditorialSummary().getOverview();
            }
            if (summary == null || summary.isEmpty()) {
                summary = item.getSummary();
            }

            // 상세 정보 (외부 API의 contents 사용)
            String information = item.getContents();

            // Place 엔티티 생성 (사진 리스트 포함)
            Place place = Place.builder()
                    .name(name)
                    .address(address)
                    .location(location)
                    .summery(summary)
                    .information(information)
                    .group(group)
                    .category(category)
                    .photos(new ArrayList<>())
                    .build();

            // Google Place에서 사진이 있으면 PlacePhoto 생성하고 Place에 추가
            if (googleCandidate.getPhotos() != null && !googleCandidate.getPhotos().isEmpty()) {
                int photoOrder = 0;

                for (GooglePlaceResponse.Photo photo : googleCandidate.getPhotos()) {
                    if (photoOrder >= 5) break; // 최대 5장까지만

                    String photoUrl = googlePlaceService.getPhotoUrl(photo.getPhotoReference(), 800);

                    PlacePhoto placePhoto = PlacePhoto.builder()
                            .place(place)
                            .photoUrl(photoUrl)
                            .order(photoOrder++)
                            .build();

                    place.getPhotos().add(placePhoto);
                }
            }

            // Place 저장 (cascade로 PlacePhoto도 함께 저장됨)
            Place savedPlace = placeRepository.save(place);

            return savedPlace;

        } catch (Exception e) {
            log.error("Place 엔티티 생성 실패: name={}, error={}", item.getTitle(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 카테고리 코드를 PlaceCategory로 매핑
     */
    private PlaceCategory mapCodeToPlaceCategory(int code) {
        return switch (code) {
            case 100 -> PlaceCategory.맛집; // 음식
            case 200 -> PlaceCategory.카페; // 숙박 -> 카페로 임시 매핑
            case 300, 400 -> PlaceCategory.관광지; // 관광명소, 문화재/역사
            default -> PlaceCategory.관광지;
        };
    }

    /**
     * 카테고리 코드를 Category로 매핑
     */
    private Category mapCodeToCategory(int code) {
        return switch (code) {
            case 100 -> Category.식도락; // 음식
            case 200 -> Category.문화_체험; // 숙박
            case 300 -> Category.자연_힐링; // 관광명소
            case 400 -> Category.역사_전통; // 문화재/역사
            default -> Category.문화_체험;
        };
    }
}
