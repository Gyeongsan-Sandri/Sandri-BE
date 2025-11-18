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
import sandri.sandriweb.domain.place.enums.DataSource;
import sandri.sandriweb.domain.place.repository.PlaceRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GBGSDataImportService {

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
            // 각 카테고리 코드별로 순회
            for (int code : CATEGORY_CODES) {
                log.info("카테고리 코드 {} 처리 시작", code);

                // 1단계: 첫 페이지를 조회하여 totalCount 확인
                GbgsTourApiResponse firstPageResponse = fetchExternalApiData(code, 1, NUM_OF_ROWS);

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
                            // Google Place API로 검색
                            GooglePlaceResponse googlePlace = googlePlaceService.findPlace(
                                item.getTitle(),
                                item.getAddress()
                            );

                            // Google 데이터에서 name과 address 추출
                            String placeName = item.getTitle();
                            String placeAddress = item.getAddress();

                            if (googlePlace != null && googlePlace.getCandidates() != null
                                && !googlePlace.getCandidates().isEmpty()) {
                                GooglePlaceResponse.Candidate candidate = googlePlace.getCandidates().get(0);
                                // Google 이름/주소 우선 사용
                                if (candidate.getName() != null && !candidate.getName().isEmpty()) {
                                    placeName = candidate.getName();
                                }
                                if (candidate.getFormattedAddress() != null && !candidate.getFormattedAddress().isEmpty()) {
                                    placeAddress = candidate.getFormattedAddress();
                                }
                            }

                            // 기존 장소 확인 (이름 + 주소)
                            java.util.Optional<Place> existingPlace = placeRepository.findByNameAndAddress(placeName, placeAddress);

                            if (existingPlace.isPresent()) {
                                // 기존 장소가 있으면 PATCH (업데이트)
                                Place place = existingPlace.get();
                                boolean updated = updatePlaceFromGbgs(place, item, googlePlace, code);
                                if (updated) {
                                    totalImported++;
                                    log.info("장소 업데이트 성공 (PATCH): {}", placeName);
                                } else {
                                    totalSkipped++;
                                    log.debug("장소 업데이트 불필요 (데이터 동일): {}", placeName);
                                }
                            } else {
                                // 새로운 장소면 POST (생성)
                                if (googlePlace != null && googlePlace.getCandidates() != null
                                    && !googlePlace.getCandidates().isEmpty()) {

                                    GooglePlaceResponse.Candidate candidate = googlePlace.getCandidates().get(0);

                                    // Place 엔티티 생성 및 저장
                                    Place savedPlace = createAndSavePlace(item, candidate, code);

                                    if (savedPlace != null) {
                                        totalImported++;
                                        log.info("장소 저장 성공 (POST): {}", placeName);
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
     * 기존 Place를 경산시 API 데이터로 업데이트 (PATCH)
     * @return true: 업데이트됨, false: 변경사항 없음
     */
    private boolean updatePlaceFromGbgs(Place place, GbgsTourApiResponse.TourItem item,
                                       GooglePlaceResponse googlePlace, int categoryCode) {
        // 좌표 업데이트 (Google 우선)
        Point newLocation = null;
        if (googlePlace != null && googlePlace.getCandidates() != null && !googlePlace.getCandidates().isEmpty()) {
            GooglePlaceResponse.Candidate candidate = googlePlace.getCandidates().get(0);
            newLocation = geometryFactory.createPoint(
                new Coordinate(
                    candidate.getGeometry().getLocation().getLng(),
                    candidate.getGeometry().getLocation().getLat()
                )
            );
        }

        // 요약 정보 업데이트
        String newSummary = null;
        if (googlePlace != null && googlePlace.getCandidates() != null && !googlePlace.getCandidates().isEmpty()) {
            GooglePlaceResponse.Candidate candidate = googlePlace.getCandidates().get(0);
            if (candidate.getEditorialSummary() != null) {
                newSummary = cleanHtmlTags(candidate.getEditorialSummary().getOverview());
            }
        }
        if (newSummary == null || newSummary.isEmpty()) {
            newSummary = cleanHtmlTags(item.getSummary());
        }

        // 상세 정보 업데이트
        String newInformation = cleanHtmlTags(item.getContents());

        // 카테고리 업데이트
        PlaceCategory newGroup = mapCodeToPlaceCategory(categoryCode);
        Category newCategory = mapCodeToCategory(categoryCode);

        // 우선순위 기반 업데이트 (GBGS가 최우선)
        return place.updateWithPriority(null, null, newLocation, newSummary,
                                       newInformation, newGroup, newCategory,
                                       DataSource.GBGS);
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
                summary = cleanHtmlTags(googleCandidate.getEditorialSummary().getOverview());
            }
            if (summary == null || summary.isEmpty()) {
                summary = cleanHtmlTags(item.getSummary());
            }

            // 상세 정보 (외부 API의 contents 사용)
            String information = cleanHtmlTags(item.getContents());

            // Place 엔티티 생성 (사진 리스트 포함)
            Place place = Place.builder()
                    .name(name)
                    .address(address)
                    .location(location)
                    .summery(summary)
                    .information(information)
                    .group(group)
                    .category(category)
                    .dataSource(DataSource.GBGS)
                    .photos(new ArrayList<>())
                    .build();

            int photoOrder = 0;

            // 1. 경산시 API 이미지 우선 추가
            String gbgsImageUrl = item.getImage() != null && !item.getImage().isEmpty()
                    ? item.getImage()
                    : item.getImageUrl();

            if (gbgsImageUrl != null && !gbgsImageUrl.isEmpty()) {
                PlacePhoto gbgsPhoto = PlacePhoto.builder()
                        .place(place)
                        .photoUrl(gbgsImageUrl)
                        .order(photoOrder++)
                        .build();
                place.getPhotos().add(gbgsPhoto);
                log.info("경산시 API 이미지 추가: {}", gbgsImageUrl);
            }

            // 2. Google Place 사진 추가 (경산시 이미지 다음에)
            if (googleCandidate.getPhotos() != null && !googleCandidate.getPhotos().isEmpty()) {
                for (GooglePlaceResponse.Photo photo : googleCandidate.getPhotos()) {
                    if (photoOrder >= 20) break; // 최대 20장까지만

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

    /**
     * HTML 태그 및 포맷 문자열 제거
     * @param text 원본 텍스트
     * @return 정제된 텍스트
     */
    private String cleanHtmlTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // HTML 태그 제거 (<br>, <p>, <div> 등)
        String cleaned = text.replaceAll("<br\\s*/?>", "\n")  // <br>을 줄바꿈으로
                             .replaceAll("<[^>]+>", "");       // 나머지 HTML 태그 제거

        // HTML 엔티티 변환
        cleaned = cleaned.replace("&nbsp;", " ")
                         .replace("&amp;", "&")
                         .replace("&lt;", "<")
                         .replace("&gt;", ">")
                         .replace("&quot;", "\"")
                         .replace("&#39;", "'")
                         .replace("&apos;", "'");

        // 연속된 줄바꿈을 2개까지만 허용 (단락 구분 유지)
        cleaned = cleaned.replaceAll("\n{3,}", "\n\n");

        // 각 줄의 앞뒤 공백 제거
        cleaned = cleaned.lines()
                         .map(String::trim)
                         .collect(java.util.stream.Collectors.joining("\n"));

        // 연속된 공백을 하나로
        cleaned = cleaned.replaceAll(" +", " ");

        // 전체 앞뒤 공백 제거
        cleaned = cleaned.trim();

        return cleaned;
    }
}
