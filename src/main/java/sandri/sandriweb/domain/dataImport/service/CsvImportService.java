package sandri.sandriweb.domain.dataImport.service;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import sandri.sandriweb.domain.dataImport.dto.GooglePlaceDetailsResponse;
import sandri.sandriweb.domain.dataImport.dto.GooglePlaceResponse;
import sandri.sandriweb.domain.dataImport.dto.StoreCsvDto;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.entity.PlacePhoto;
import sandri.sandriweb.domain.place.enums.Category;
import sandri.sandriweb.domain.place.enums.DataSource;
import sandri.sandriweb.domain.place.enums.PlaceCategory;
import sandri.sandriweb.domain.place.repository.PlaceRepository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CsvImportService {

    private final PlaceRepository placeRepository;
    private final GooglePlaceService googlePlaceService;
    private final CsvImportService self;  // Self-injection for @Transactional(REQUIRES_NEW)

    public CsvImportService(PlaceRepository placeRepository,
                           GooglePlaceService googlePlaceService,
                           @org.springframework.context.annotation.Lazy CsvImportService self) {
        this.placeRepository = placeRepository;
        this.googlePlaceService = googlePlaceService;
        this.self = self;
    }

    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final String TARGET_PROVINCE = "경상북도";
    private static final String TARGET_CITY = "경산시";

    /**
     * CSV 파일에서 경산시 매장 정보를 추출하여 DB에 저장
     * @param csvFile CSV 파일
     * @param mode "insert" (신규만 추가) 또는 "upsert" (업데이트 포함)
     * @return 처리 결과 메시지
     */
    public String importStoresFromCsv(MultipartFile csvFile, String mode) {
        int totalImported = 0;
        int totalFailed = 0;
        int totalSkipped = 0;
        int totalFiltered = 0;

        log.info("CSV 파일 임포트 시작: filename={}, mode={}", csvFile.getOriginalFilename(), mode);

        try {
            // CSV 파일 파싱
            List<StoreCsvDto> stores = parseCsvFile(csvFile);
            log.info("CSV 파일 파싱 완료: 총 {} 개 행", stores.size());

            // 경산시 필터링
            List<StoreCsvDto> filteredStores = stores.stream()
                    .filter(store -> TARGET_PROVINCE.equals(store.getProvince())
                                  && TARGET_CITY.equals(store.getCity()))
                    .collect(Collectors.toList());

            totalFiltered = stores.size() - filteredStores.size();
            log.info("필터링 완료: {} 개 매장 (제외: {} 개)", filteredStores.size(), totalFiltered);

            // 각 매장 처리
            for (StoreCsvDto store : filteredStores) {
                try {
                    boolean success = self.processStore(store, mode);  // Self-injection으로 호출
                    if (success) {
                        totalImported++;
                    } else {
                        totalSkipped++;
                    }
                } catch (Exception e) {
                    log.error("매장 처리 중 오류: name={}, error={}", store.getStoreName(), e.getMessage());
                    totalFailed++;
                }
            }

            String result = String.format("CSV 임포트 완료 - 성공: %d, 실패: %d, 유지: %d, 경산시 외 제외: %d",
                totalImported, totalFailed, totalSkipped, totalFiltered);
            log.info(result);
            return result;

        } catch (Exception e) {
            log.error("CSV 임포트 중 오류 발생", e);
            throw new RuntimeException("CSV 임포트 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 개별 매장 처리 (독립적인 트랜잭션)
     * @param mode "insert" (신규만 추가) 또는 "upsert" (업데이트 포함)
     * @return true: 저장 성공, false: 스킵
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public boolean processStore(StoreCsvDto store, String mode) {
        // 상호명 생성 (지점명 포함)
        String fullName = store.getStoreName();
        if (store.getBranchName() != null && !store.getBranchName().isEmpty()) {
            fullName = store.getStoreName() + " " + store.getBranchName();
        }

        // 주소 (도로명 주소 우선)
        String address = store.getRoadAddress() != null && !store.getRoadAddress().isEmpty()
                ? store.getRoadAddress()
                : store.getJibunAddress();

        // 기존 장소 확인 (이름 + 주소)
        java.util.Optional<Place> existingPlace = placeRepository.findByNameAndAddress(fullName, address);

        // 1단계: Google Find Place API로 place_id 검색
        GooglePlaceResponse googlePlace = googlePlaceService.findPlace(fullName, address);

        // 2단계: Place Details API로 상세 정보 조회
        GooglePlaceDetailsResponse placeDetails = null;
        if (googlePlace != null && googlePlace.getCandidates() != null
                && !googlePlace.getCandidates().isEmpty()) {
            String placeId = googlePlace.getCandidates().get(0).getPlaceId();
            if (placeId != null && !placeId.isEmpty()) {
                placeDetails = googlePlaceService.getPlaceDetails(placeId);
            }
        }

        if (existingPlace.isPresent()) {
            // 기존 장소가 있음
            if ("insert".equals(mode)) {
                // insert 모드: 업데이트 안 함, 유지
                log.debug("기존 장소 유지 (insert 모드): {}", fullName);
                return false;
            } else {
                // upsert 모드: 업데이트 시도
                Place place = existingPlace.get();
                boolean updated = updatePlaceFromCsv(place, store, placeDetails);
                if (updated) {
                    log.info("장소 업데이트 성공 (PATCH): {}", fullName);
                    return true;
                } else {
                    log.debug("장소 업데이트 불필요 (데이터 동일): {}", fullName);
                    return false;
                }
            }
        } else {
            // 새로운 장소면 POST (생성)
            if (placeDetails != null) {
                // Place 엔티티 생성 및 저장 (Google 데이터 기반)
                Place savedPlace = createAndSavePlaceFromCsv(store, placeDetails);

                if (savedPlace != null) {
                    log.info("장소 저장 성공 (POST - Google 데이터): {}", fullName);
                    return true;
                }
            } else {
                // Google Place API에서 찾을 수 없는 경우 CSV 데이터로 직접 저장
                log.info("Google Place API에서 찾을 수 없음. CSV 데이터로 저장 시도: {}", fullName);
                Place savedPlace = createAndSavePlaceFromCsvOnly(store);

                if (savedPlace != null) {
                    log.info("장소 저장 성공 (POST - CSV 데이터): {}", fullName);
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 기존 Place를 CSV 데이터로 업데이트 (PATCH)
     * @return true: 업데이트됨, false: 변경사항 없음
     */
    private boolean updatePlaceFromCsv(Place place, StoreCsvDto csvData, GooglePlaceDetailsResponse placeDetails) {
        // 데이터 소스 결정 (Google 데이터가 있으면 GOOGLE, 없으면 CSV)
        DataSource dataSource;
        boolean hasGoogleData = placeDetails != null && placeDetails.getLocation() != null;
        dataSource = hasGoogleData ? DataSource.GOOGLE : DataSource.CSV;

        // 좌표 업데이트
        Point newLocation = null;
        if (hasGoogleData) {
            // Google 좌표 우선
            newLocation = geometryFactory.createPoint(
                new Coordinate(
                    placeDetails.getLocation().getLongitude(),
                    placeDetails.getLocation().getLatitude()
                )
            );
        } else if (csvData.getLongitude() != null && !csvData.getLongitude().isEmpty()
                && csvData.getLatitude() != null && !csvData.getLatitude().isEmpty()) {
            // CSV 좌표 사용
            try {
                double longitude = Double.parseDouble(csvData.getLongitude());
                double latitude = Double.parseDouble(csvData.getLatitude());
                newLocation = geometryFactory.createPoint(new Coordinate(longitude, latitude));
            } catch (NumberFormatException e) {
                log.warn("좌표 변환 실패: lng={}, lat={}", csvData.getLongitude(), csvData.getLatitude());
            }
        }

        // 요약 정보 업데이트 (Google editorialSummary 우선, 없으면 상권업종소분류명)
        String newSummary = null;
        if (hasGoogleData) {
            if (placeDetails.getEditorialSummary() != null) {
                newSummary = cleanHtmlTags(placeDetails.getEditorialSummary().getText());
            }
        }
        if (newSummary == null || newSummary.isEmpty()) {
            newSummary = cleanHtmlTags(csvData.getIndustryName());
        }

        // 상세 정보 업데이트 (CSV에는 information에 넣을 내용이 없으므로 null)
        String newInformation = null;

        // 카테고리 업데이트
        PlaceCategory newGroup = mapIndustryCodeToPlaceCategory(csvData.getIndustryCode());
        Category newCategory = mapIndustryCodeToCategory(csvData.getIndustryCode());

        // 우선순위 기반 업데이트
        return place.updateWithPriority(null, null, newLocation, newSummary,
                                       newInformation, newGroup, newCategory,
                                       dataSource);
    }

    /**
     * CSV 파일 파싱
     */
    private List<StoreCsvDto> parseCsvFile(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            CsvToBean<StoreCsvDto> csvToBean = new CsvToBeanBuilder<StoreCsvDto>(reader)
                    .withType(StoreCsvDto.class)
                    .withIgnoreLeadingWhiteSpace(true)
                    .build();

            return csvToBean.parse();

        } catch (Exception e) {
            log.error("CSV 파일 파싱 실패", e);
            throw new RuntimeException("CSV 파일 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * Place 엔티티 생성 및 저장 (CSV 데이터 기반)
     */
    private Place createAndSavePlaceFromCsv(StoreCsvDto csvData, GooglePlaceDetailsResponse placeDetails) {
        try {
            // 좌표 생성 (Google Place Details API 기준)
            Point location = geometryFactory.createPoint(
                new Coordinate(
                    placeDetails.getLocation().getLongitude(),
                    placeDetails.getLocation().getLatitude()
                )
            );

            // 상호명 (지점명 포함)
            String fullName = csvData.getStoreName();
            if (csvData.getBranchName() != null && !csvData.getBranchName().isEmpty()) {
                fullName = csvData.getStoreName() + " " + csvData.getBranchName();
            }

            // Google 이름 우선
            String name = placeDetails.getDisplayName() != null && placeDetails.getDisplayName().getText() != null
                    ? placeDetails.getDisplayName().getText()
                    : fullName;

            // 주소 (Google 우선)
            String address = placeDetails.getFormattedAddress() != null && !placeDetails.getFormattedAddress().isEmpty()
                    ? placeDetails.getFormattedAddress()
                    : (csvData.getRoadAddress() != null && !csvData.getRoadAddress().isEmpty()
                        ? csvData.getRoadAddress()
                        : csvData.getJibunAddress());

            // 요약 정보 (Google editorialSummary 우선, 없으면 CSV의 상권업종소분류명)
            String summary = null;
            if (placeDetails.getEditorialSummary() != null && placeDetails.getEditorialSummary().getText() != null) {
                summary = cleanHtmlTags(placeDetails.getEditorialSummary().getText());
            }
            if (summary == null || summary.isEmpty()) {
                summary = cleanHtmlTags(csvData.getIndustryName());  // 상권업종소분류명
            }

            // 상세 정보 (CSV에는 information에 넣을 내용이 없으므로 null)
            String information = null;

            // 카테고리 매핑 (업종코드 기반)
            PlaceCategory group = mapIndustryCodeToPlaceCategory(csvData.getIndustryCode());
            Category category = mapIndustryCodeToCategory(csvData.getIndustryCode());

            // 매핑되지 않은 업종코드는 스킵
            if (group == null || category == null) {
                log.info("업종코드 매핑 안 됨, 스킵: code={}, name={}", csvData.getIndustryCode(), name);
                return null;
            }

            // Place 엔티티 생성 (사진 리스트 포함)
            Place place = Place.builder()
                    .name(name)
                    .address(address)
                    .location(location)
                    .summery(summary)
                    .information(information)
                    .group(group)
                    .category(category)
                    .dataSource(DataSource.GOOGLE)
                    .photos(new ArrayList<>())
                    .build();

            // Google Place Details에서 사진이 있으면 PlacePhoto 생성 (최대 10장)
            if (placeDetails.getPhotos() != null && !placeDetails.getPhotos().isEmpty()) {
                int photoOrder = 0;

                for (GooglePlaceDetailsResponse.Photo photo : placeDetails.getPhotos()) {
                    if (photoOrder >= 10) break; // 최대 10장까지

                    // New Places API photo URL 생성
                    String photoUrl = googlePlaceService.getPhotoUrlFromName(photo.getName(), 800);

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
            log.error("Place 엔티티 생성 실패: name={}, error={}", csvData.getStoreName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Place 엔티티 생성 및 저장 (CSV 데이터만 사용)
     */
    private Place createAndSavePlaceFromCsvOnly(StoreCsvDto csvData) {
        try {
            // CSV 좌표 검증
            if (csvData.getLongitude() == null || csvData.getLongitude().isEmpty()
                || csvData.getLatitude() == null || csvData.getLatitude().isEmpty()) {
                log.warn("CSV 데이터에 좌표 정보 없음: {}", csvData.getStoreName());
                return null;
            }

            // 좌표 생성 (CSV 데이터 기준)
            double longitude = Double.parseDouble(csvData.getLongitude());
            double latitude = Double.parseDouble(csvData.getLatitude());
            Point location = geometryFactory.createPoint(
                new Coordinate(longitude, latitude)
            );

            // 상호명 (지점명 포함)
            String fullName = csvData.getStoreName();
            if (csvData.getBranchName() != null && !csvData.getBranchName().isEmpty()) {
                fullName = csvData.getStoreName() + " " + csvData.getBranchName();
            }

            // 주소 (도로명 주소 우선)
            String address = csvData.getRoadAddress() != null && !csvData.getRoadAddress().isEmpty()
                    ? csvData.getRoadAddress()
                    : csvData.getJibunAddress();

            // 요약 정보 (상권업종소분류명)
            String summary = cleanHtmlTags(csvData.getIndustryName());

            // 상세 정보 (CSV에는 information에 넣을 내용이 없으므로 null)
            String information = null;

            // 카테고리 매핑 (업종코드 기반)
            PlaceCategory group = mapIndustryCodeToPlaceCategory(csvData.getIndustryCode());
            Category category = mapIndustryCodeToCategory(csvData.getIndustryCode());

            // 매핑되지 않은 업종코드는 스킵
            if (group == null || category == null) {
                log.info("업종코드 매핑 안 됨, 스킵: code={}, name={}", csvData.getIndustryCode(), fullName);
                return null;
            }

            // Place 엔티티 생성
            Place place = Place.builder()
                    .name(fullName)
                    .address(address)
                    .location(location)
                    .summery(summary)  // 상권업종소분류명
                    .information(information)
                    .group(group)
                    .category(category)
                    .dataSource(DataSource.CSV)
                    .build();

            return placeRepository.save(place);

        } catch (NumberFormatException e) {
            log.error("좌표 변환 실패: name={}, lng={}, lat={}",
                csvData.getStoreName(), csvData.getLongitude(), csvData.getLatitude(), e);
            return null;
        } catch (Exception e) {
            log.error("Place 엔티티 생성 실패 (CSV 전용): name={}, error={}",
                csvData.getStoreName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 업종코드를 PlaceCategory로 매핑
     * @return PlaceCategory 또는 null (매핑되지 않은 코드)
     */
    private PlaceCategory mapIndustryCodeToPlaceCategory(String industryCode) {
        if (industryCode == null || industryCode.isEmpty()) {
            return null;  // 업종코드 없음 → 스킵
        }

        // 필요시설
        if (Set.of("G20404", "G20405", "G20499", "G20507", "G21501", "M11101",
                   "Q10101", "Q10102", "Q10103", "Q10204", "Q10205", "Q10206",
                   "Q10207", "Q10208", "Q10209", "Q10210", "Q10211", "Q10212").contains(industryCode)) {
            return PlaceCategory.필요시설;
        }

        // 쇼핑
        if (Set.of("G20901", "G20902", "G20903", "G20904", "G20905", "G20907",
                   "G20910", "G20911", "G21701", "G21901", "N11004",
                   "S20701", "S20702", "S20703").contains(industryCode)) {
            return PlaceCategory.쇼핑;
        }

        // 취미
        if (Set.of("G20909", "G21301", "G21302", "G21303", "G21801", "G21802",
                   "I10104", "I10299", "N11001", "N11003", "P10613",
                   "R10306", "R10307", "R10309", "R10310", "R10311",
                   "I10901", "R10312", "R10313", "R10314", "R10316",
                   "R10402", "R10404", "R10405", "R10406", "R10407", "R10408", "R10409", "R10499").contains(industryCode)) {
            return PlaceCategory.취미;
        }

        // 맛집
        if (Set.of("I20101", "I20102", "I20103", "I20104", "I20105", "I21016",
                   "I20107", "I20108", "I20109", "I20110", "I20111", "I20112",
                   "I20113", "I20199", "I20201", "I20202", "I20301", "I20302",
                   "I20303", "I20399", "I20401", "I20402", "I20403", "I20499",
                   "I20501", "I20599", "I20601", "I20702", "I21001", "I21002",
                   "I21003", "I21004", "I21005", "I21006", "I21007", "I21008",
                   "I21099", "I21103", "I21104").contains(industryCode)) {
            return PlaceCategory.맛집;
        }

        // 카페
        if ("I121202".equals(industryCode)) {
            return PlaceCategory.카페;
        }

        // 숙박시설
        if (Set.of("I10101", "I10102", "I10103", "S20901", "S29002").contains(industryCode)) {
            return PlaceCategory.숙박시설;
        }

        return null;  // 매핑되지 않은 업종코드 → 스킵
    }

    /**
     * 업종코드를 Category로 매핑
     * @return Category 또는 null (매핑되지 않은 코드)
     */
    private Category mapIndustryCodeToCategory(String industryCode) {
        if (industryCode == null || industryCode.isEmpty()) {
            return null;  // 업종코드 없음 → 스킵
        }

        // I로 시작하면 음식 관련
        if (industryCode.startsWith("I")) {
            return Category.식도락;
        }

        return Category.문화_체험;
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
