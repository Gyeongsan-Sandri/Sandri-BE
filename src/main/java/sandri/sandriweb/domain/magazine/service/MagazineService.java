package sandri.sandriweb.domain.magazine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import sandri.sandriweb.domain.magazine.dto.*;
import sandri.sandriweb.domain.magazine.entity.Magazine;
import sandri.sandriweb.domain.magazine.entity.MagazineCard;
import sandri.sandriweb.domain.magazine.entity.Tag;
import sandri.sandriweb.domain.magazine.entity.mapping.MagazineTag;
import sandri.sandriweb.domain.magazine.entity.mapping.UserMagazine;
import sandri.sandriweb.domain.magazine.repository.MagazineCardRepository;
import sandri.sandriweb.domain.magazine.repository.MagazineRepository;
import sandri.sandriweb.domain.magazine.repository.MagazineTagRepository;
import sandri.sandriweb.domain.magazine.repository.TagRepository;
import sandri.sandriweb.domain.magazine.repository.UserMagazineRepository;
import sandri.sandriweb.domain.place.dto.SimplePlaceDto;
import sandri.sandriweb.domain.place.entity.Place;
import sandri.sandriweb.domain.place.repository.PlacePhotoRepository;
import sandri.sandriweb.domain.place.repository.PlaceRepository;
import sandri.sandriweb.domain.place.repository.UserPlaceRepository;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MagazineService {

    private final MagazineRepository magazineRepository;
    private final MagazineCardRepository magazineCardRepository;
    private final UserMagazineRepository userMagazineRepository;
    private final UserRepository userRepository;
    private final PlacePhotoRepository placePhotoRepository;
    private final PlaceRepository placeRepository;
    private final UserPlaceRepository userPlaceRepository;
    private final TagRepository tagRepository;
    private final MagazineTagRepository magazineTagRepository;

    /*
     * 매거진 상세 조회 (카드뉴스 포함)
     * @param magazineId 매거진 ID
     * @return 매거진 상세 정보와 카드뉴스 리스트
     */
    @Transactional(readOnly = true)
    public MagazineDetailResponseDto getMagazineDetail(Long magazineId, Long userId) {
        // 1. 매거진과 카드를 함께 조회 (FETCH JOIN)
        Magazine magazine = magazineRepository.findByIdWithCards(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));

        // 2. MagazineCard를 DTO로 변환 (DB에서 이미 enabled 필터링 및 order 정렬됨)
        // order를 0부터 연속적으로 재정렬 (불연속적인 order를 프론트엔드에 전달하지 않음)
        List<MagazineCardDto> cardDtos = new ArrayList<>();
        if (magazine.getCards() != null && !magazine.getCards().isEmpty()) {
            int index = 0;
            for (MagazineCard card : magazine.getCards()) {
                cardDtos.add(MagazineCardDto.builder()
                        .order(index) // 0부터 연속적으로 재정렬
                        .cardUrl(card.getCardUrl())
                        .build());
                index++;
            }
        }

        // 3. 사용자 좋아요 여부 조회 (로그인한 경우)
        Boolean isLiked = null;
        if (userId != null) {
            List<Long> likedIds = userMagazineRepository.findLikedMagazineIdsByUserId(userId, List.of(magazineId));
            isLiked = likedIds.contains(magazineId);
        }

        log.info("매거진 상세 조회: magazineId={}, cardCount={}, totalCardsFetched={}, isLiked={}", 
                 magazineId, cardDtos.size(), 
                 magazine.getCards() != null ? magazine.getCards().size() : 0,
                 isLiked);

        // DTO 생성 및 반환
        return MagazineDetailResponseDto.builder()
                .magazineId(magazine.getId())
                .content(magazine.getContent())
                .cardCount(cardDtos.size())
                .isLiked(isLiked)
                .cards(cardDtos)
                .build();
    }

    /*
     * 커서 기반 매거진 목록 조회 (썸네일만 포함)
     * @param lastMagazineId 마지막으로 조회한 매거진 ID (첫 조회시 null)
     * @param size 페이지 크기
     * @param userId 사용자 ID (로그인한 경우에만 제공, null 가능)
     * @return 커서 기반 페이징된 매거진 목록 (제목, 썸네일, 요약, 좋아요 여부)
     */
    @Transactional(readOnly = true)
    public MagazineListCursorResponseDto getMagazineListByCursor(Long lastMagazineId, int size, Long userId) {
        // 1. size + 1개 조회하여 다음 페이지 여부 판단
        Pageable pageable = PageRequest.of(0, size + 1);
        List<Magazine> all = magazineRepository.findEnabledWithThumbnailByCursor(lastMagazineId, pageable);

        boolean hasNext = all.size() > size;
        List<Magazine> pageItems = hasNext ? all.subList(0, size) : all;

        // 2. 사용자가 좋아요한 매거진 ID 조회 (로그인한 경우)
        List<Long> magazineIds = pageItems.stream().map(Magazine::getId).collect(Collectors.toList());
        Map<Long, Boolean> likedMagazineIds;
        if (userId != null) {
            List<Long> likedIds = userMagazineRepository.findLikedMagazineIdsByUserId(userId, magazineIds);
            likedMagazineIds = likedIds.stream()
                    .collect(Collectors.toMap(
                            magazineId -> magazineId,
                            magazineId -> true
                    ));
        } else {
            likedMagazineIds = new HashMap<>();
        }

        // 3. 매거진 태그를 batch fetch (N+1 문제 방지)
        List<MagazineTag> magazineTags = magazineTagRepository.findByMagazineIdInWithTag(magazineIds);
        Map<Long, List<MagazineTag>> tagsByMagazineId = magazineTags.stream()
                .collect(Collectors.groupingBy(mt -> mt.getMagazine().getId()));

        // 4. DTO 변환
        List<MagazineListDto> content = pageItems.stream()
                .map(magazine -> {
                    // 썸네일 가져오기 (order = 0인 카드만 fetch join되어 있음)
                    String thumbnail = null;
                    if (magazine.getCards() != null && !magazine.getCards().isEmpty()) {
                        // fetch join으로 가져온 카드는 order = 0이고 enabled = true인 카드만 존재
                        thumbnail = magazine.getCards().get(0).getCardUrl();
                    }

                    // 사용자가 좋아요한 매거진인지 확인
                    Boolean isLiked = userId != null ? likedMagazineIds.getOrDefault(magazine.getId(), false) : null;

                    // 태그 변환 (batch fetch한 데이터 사용)
                    List<TagDto> tagDtos = new ArrayList<>();
                    List<MagazineTag> magazineTagList = tagsByMagazineId.getOrDefault(magazine.getId(), new ArrayList<>());
                    if (!magazineTagList.isEmpty()) {
                        tagDtos = magazineTagList.stream()
                                .map(MagazineTag::getTag)
                                .filter(tag -> tag != null && tag.isEnabled()) // enabled된 태그만
                                .map(tag -> TagDto.builder()
                                        .tagId(tag.getId())
                                        .name(tag.getName())
                                        .build())
                                .collect(Collectors.toList());
                    }

                    return MagazineListDto.builder()
                            .magazineId(magazine.getId())
                            .title(magazine.getName())
                            .thumbnail(thumbnail)
                            .summary(magazine.getSummary())
                            .isLiked(isLiked)
                            .tags(tagDtos)
                            .build();
                })
                .collect(Collectors.toList());

        // 다음 커서 설정 (hasNext가 true이고 항목이 있을 때만 마지막 항목의 ID를 커서로 사용)
        Long nextCursor = (hasNext && !pageItems.isEmpty()) 
                ? pageItems.get(pageItems.size() - 1).getId() 
                : null;

        // 전체 매거진 개수 조회
        long totalCount = magazineRepository.countByEnabledTrue();

        return MagazineListCursorResponseDto.builder()
                .magazines(content)
                .size(size)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .totalCount(totalCount)
                .build();
    }

    /*
     * 매거진 좋아요 토글
     * @param magazineId 매거진 ID
     * @param userId 사용자 ID
     * @return 좋아요 상태 (true: 좋아요 활성화, false: 좋아요 비활성화)
     */
    @Transactional
    public boolean toggleLike(Long magazineId, Long userId) {
        // 매거진 존재 확인
        if (!magazineRepository.existsById(magazineId)) {
            throw new RuntimeException("매거진을 찾을 수 없습니다.");
        }
        
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("사용자를 찾을 수 없습니다.");
        }
        
        // 기존 좋아요 조회
        return userMagazineRepository.findByUserIdAndMagazineId(userId, magazineId)
                .map(userMagazine -> {
                    // 이미 좋아요가 있는 경우: 토글
                    if (userMagazine.isEnabled()) {
                        userMagazine.disable(); // 좋아요 취소
                        userMagazineRepository.save(userMagazine);
                        return false;
                    } else {
                        userMagazine.enable(); // 좋아요 재활성화
                        userMagazineRepository.save(userMagazine);
                        return true;
                    }
                })
                .orElseGet(() -> {
                    // 좋아요가 없는 경우: 새로 생성 (User와 Magazine은 프록시로 로드)
                    User user = userRepository.getReferenceById(userId);
                    Magazine magazine = magazineRepository.getReferenceById(magazineId);
                    UserMagazine newUserMagazine = UserMagazine.builder()
                            .user(user)
                            .magazine(magazine)
                            .build();
                    userMagazineRepository.save(newUserMagazine);
                    return true;
                });
    }

    // createMagazineCards 헬퍼 메소드    
    private List<MagazineCard> createMagazineCards(Magazine magazine, List<MagazineCardInfoDto> cardInfos) {
        if (cardInfos == null || cardInfos.isEmpty()) {
            return List.of();
        }

        List<MagazineCard> cards = new ArrayList<>();
        for (MagazineCardInfoDto cardInfo : cardInfos) {
            // 카드 URL 중복 검사
            if (magazineCardRepository.existsByCardUrl(cardInfo.getCardUrl())) {
                throw new RuntimeException("이미 존재하는 카드 이미지 URL입니다: " + cardInfo.getCardUrl());
            }

            MagazineCard card = MagazineCard.builder()
                    .magazine(magazine)
                    .order(cardInfo.getOrder())
                    .cardUrl(cardInfo.getCardUrl())
                    .enabled(true)
                    .build();
            cards.add(card);
        }
        return cards;
    }

    /*
     * 매거진 생성 (관리자용)
     * @param request 매거진 생성 요청 DTO
     * @return 생성된 매거진 ID
     */
    @Transactional
    public Long createMagazine(CreateMagazineRequestDto request) {
        // 1. 매거진 이름 중복 검사
        if (magazineRepository.existsByName(request.getName())) {
            throw new RuntimeException("이미 존재하는 매거진 이름입니다: " + request.getName());
        }

        // 2. 매거진 생성 (enabled = true로 명시적 설정)
        Magazine magazine = Magazine.builder()
                .name(request.getName())
                .summary(request.getSummary())
                .content(request.getContent())
                .enabled(true) // 명시적으로 활성화
                .build();

        Magazine savedMagazine = magazineRepository.save(magazine);

        // 2. 매거진 카드 생성 (order와 cardUrl 사용)
        List<MagazineCard> cards = createMagazineCards(savedMagazine, request.getCards());
        if (!cards.isEmpty()) {
            magazineCardRepository.saveAll(cards);
            log.info("매거진 카드 {}개 추가 완료: magazineId={}", cards.size(), savedMagazine.getId());
        }

        log.info("매거진 생성 완료: magazineId={}, name={}, cardCount={}",
                 savedMagazine.getId(), savedMagazine.getName(),
                 request.getCards() != null ? request.getCards().size() : 0);

        return savedMagazine.getId();
    }

    /*
     * 매거진 수정 (관리자용)
     * @param magazineId 매거진 ID
     * @param request 매거진 수정 요청 DTO
     * @return 수정된 매거진 ID
     */
    @Transactional
    public Long updateMagazine(Long magazineId, UpdateMagazineRequestDto request) {
        // 매거진 조회 (카드 포함)
        Magazine magazine = magazineRepository.findByIdWithCards(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));

        // 1. 매거진 이름 중복 검사 (이름이 변경되는 경우에만)
        if (!magazine.getName().equals(request.getName()) && 
            magazineRepository.existsByName(request.getName())) {
            throw new RuntimeException("이미 존재하는 매거진 이름입니다: " + request.getName());
        }

        // 2. 매거진 정보 수정
        magazine.update(request.getName(), request.getSummary(), request.getContent());

        // 3. 카드 업데이트 (요청에 cards가 포함된 경우)
        if (request.getCards() != null && !request.getCards().isEmpty()) {
            // 기존 enabled된 카드 조회
            Map<Integer, MagazineCard> existingCardsByOrder = new HashMap<>();
            if (magazine.getCards() != null && !magazine.getCards().isEmpty()) {
                existingCardsByOrder = magazine.getCards().stream()
                        .filter(card -> card.isEnabled())
                        .collect(Collectors.toMap(
                                MagazineCard::getOrder,
                                card -> card,
                                (existing, replacement) -> existing // 중복 시 기존 것 유지
                        ));
            }

            // 요청된 카드 정보로 업데이트 또는 생성
            List<MagazineCard> cardsToSave = new ArrayList<>();
            for (MagazineCardInfoDto cardInfo : request.getCards()) {
                Integer order = cardInfo.getOrder();
                String cardUrl = cardInfo.getCardUrl();
                
                // cardUrl이 빈 문자열이면 disable 처리
                if (cardUrl != null && cardUrl.trim().isEmpty()) {
                    MagazineCard existingCard = existingCardsByOrder.get(order);
                    if (existingCard != null) {
                        existingCard.disable();
                        cardsToSave.add(existingCard);
                    }
                } else if (cardUrl != null && !cardUrl.trim().isEmpty()) {
                    // cardUrl이 있으면 업데이트 또는 생성
                    MagazineCard existingCard = existingCardsByOrder.get(order);
                    if (existingCard != null) {
                        // 기존 카드가 있으면 URL만 업데이트하고 enable (URL이 변경되는 경우에만 중복 검사)
                        if (!existingCard.getCardUrl().equals(cardUrl) && 
                            magazineCardRepository.existsByCardUrl(cardUrl)) {
                            throw new RuntimeException("이미 존재하는 카드 이미지 URL입니다: " + cardUrl);
                        }
                        existingCard.updateCardUrl(cardUrl);
                        existingCard.enable();
                        cardsToSave.add(existingCard);
                    } else {
                        // 기존 카드가 없으면 새로 생성 (중복 검사)
                        if (magazineCardRepository.existsByCardUrl(cardUrl)) {
                            throw new RuntimeException("이미 존재하는 카드 이미지 URL입니다: " + cardUrl);
                        }
                        MagazineCard newCard = MagazineCard.builder()
                                .magazine(magazine)
                                .order(order)
                                .cardUrl(cardUrl)
                                .enabled(true)
                                .build();
                        magazine.getCards().add(newCard);
                        cardsToSave.add(newCard);
                    }
                }
            }
            
            // 변경사항 저장
            if (!cardsToSave.isEmpty()) {
                magazineCardRepository.saveAll(cardsToSave);
            }

            log.info("매거진 카드 업데이트 완료: magazineId={}, processedCount={}", 
                     magazineId, request.getCards().size());
        }

        log.info("매거진 수정 완료: magazineId={}, name={}",
                 magazine.getId(), magazine.getName());

        return magazine.getId();
    }

    /**
     * 매거진의 카드에서 Place 목록 추출 (공통 로직)
     * @param magazine 매거진 엔티티
     * @param limit 제한할 개수 (null이면 제한 없음)
     * @return Place 목록
     */
    private List<Place> extractPlacesFromMagazine(Magazine magazine, Integer limit) {
        if (magazine.getCards() == null || magazine.getCards().isEmpty()) {
            return List.of();
        }

        // Place ID 기준으로 중복 제거 (같은 Place가 여러 카드에 매핑될 수 있으므로)
        Map<Long, Place> placeMap = new HashMap<>();
        for (MagazineCard card : magazine.getCards()) {
            Place place = card.getPlace();
            if (place != null && place.isEnabled()) {
                placeMap.putIfAbsent(place.getId(), place); // ID 기준으로 unique하게 유지
            }
        }

        List<Place> places = new ArrayList<>(placeMap.values());

        if (limit != null && limit > 0) {
            places = places.stream()
                    .limit(limit)
                    .collect(Collectors.toList());
        }

        return places;
    }

    /**
     * Place 목록에서 ID 리스트 추출 및 사진 URL 조회 (공통 로직)
     * @param places Place 목록
     * @return Place ID를 키로, 사진 URL을 값으로 하는 Map
     */
    private Map<Long, String> getPhotoUrlMapFromPlaces(List<Place> places) {
        if (places == null || places.isEmpty()) {
            return new HashMap<>();
        }

        List<Long> placeIds = places.stream()
                .map(Place::getId)
                .collect(Collectors.toList());

        return getPhotoUrlByPlaceIds(placeIds);
    }

    /**
     * 매거진의 각 카드뉴스에 매핑된 Place 목록 조회
     * @param magazineId 매거진 ID
     * @param userId 사용자 ID (로그인한 경우에만 제공, null 가능)
     * @return 매핑된 Place 목록 (SimplePlaceDto)
     */
    @Transactional(readOnly = true)
    public List<SimplePlaceDto> getPlacesByMagazineId(Long magazineId, Long userId) {
        // 1. 매거진과 카드, Place를 함께 조회 (FETCH JOIN)
        Magazine magazine = magazineRepository.findByIdWithCardsAndPlaces(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));

        // 2. 카드에서 Place 추출
        List<Place> places = extractPlacesFromMagazine(magazine, null);

        if (places.isEmpty()) {
            return List.of();
        }

        // 3. 사진 조회 및 매핑
        Map<Long, String> photoUrlByPlaceId = getPhotoUrlMapFromPlaces(places);

        // 4. Place ID 리스트
        List<Long> placeIds = places.stream()
                .map(Place::getId)
                .collect(Collectors.toList());

        // 5. 사용자가 좋아요한 장소 ID 조회 (로그인한 경우)
        Map<Long, Boolean> likedPlaceIds;
        if (userId != null) {
            List<Long> likedIds = userPlaceRepository.findLikedPlaceIdsByUserId(userId, placeIds);
            likedPlaceIds = likedIds.stream()
                    .collect(Collectors.toMap(
                            placeId -> placeId,
                            placeId -> true
                    ));
        } else {
            likedPlaceIds = new HashMap<>();
        }

        // 6. DTO 변환
        return places.stream()
                .map(place -> {
                    // 사진 URL 추출
                    String thumbnailUrl = photoUrlByPlaceId.get(place.getId());

                    // 사용자가 좋아요한 장소인지 확인
                    Boolean isLiked = userId != null ? likedPlaceIds.getOrDefault(place.getId(), false) : null;

                    return SimplePlaceDto.builder()
                            .placeId(place.getId())
                            .name(place.getName())
                            .address(place.getAddress())
                            .thumbnailUrl(thumbnailUrl)
                            .isLiked(isLiked)
                            .groupName(place.getGroup() != null ? place.getGroup().name() : null)
                            .categoryName(place.getCategory() != null ? place.getCategory().getDisplayName() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 매거진의 각 카드뉴스에 매핑된 Place 목록 조회 (이름, 썸네일만, 개수 제한)
     * @param magazineId 매거진 ID
     * @param count 조회할 개수
     * @return 매핑된 Place 목록 (MagazinePlaceThumbnailDto)
     */
    @Transactional(readOnly = true)
    public List<MagazinePlaceThumbnailDto> getPlaceThumbnailsByMagazineId(Long magazineId, int count) {
        // 1. 매거진과 카드, Place를 함께 조회 (FETCH JOIN)
        Magazine magazine = magazineRepository.findByIdWithCardsAndPlaces(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));

        // 2. 카드에서 Place 추출 (개수 제한)
        List<Place> places = extractPlacesFromMagazine(magazine, count);

        if (places.isEmpty()) {
            return List.of();
        }

        // 3. 사진 조회 및 매핑
        Map<Long, String> photoUrlByPlaceId = getPhotoUrlMapFromPlaces(places);

        // 5. DTO 변환
        return places.stream()
                .map(place -> {
                    // 사진 URL 추출
                    String thumbnailUrl = photoUrlByPlaceId.get(place.getId());

                    return MagazinePlaceThumbnailDto.builder()
                            .placeId(place.getId())
                            .name(place.getName())
                            .thumbnailUrl(thumbnailUrl)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 여러 장소의 첫 번째 사진 URL을 조회하여 Place ID별로 매핑
     * N+1 문제 방지를 위해 배치 조회 사용
     * @param placeIds 장소 ID 목록
     * @return Place ID를 키로, 사진 URL을 값으로 하는 Map
     */
    private Map<Long, String> getPhotoUrlByPlaceIds(List<Long> placeIds) {
        if (placeIds == null || placeIds.isEmpty()) {
            return new HashMap<>();
        }
        
        // 배치 조회로 각 장소당 첫 번째 사진만 조회 (N+1 문제 방지)
        List<Object[]> photoResults = placePhotoRepository.findFirstPhotoUrlByPlaceIdIn(placeIds);
        
        return photoResults.stream()
                .collect(Collectors.toMap(
                        result -> ((Number) result[0]).longValue(), // place_id
                        result -> (String) result[1]  // photo_url
                ));
    }

    /**
     * 전체 태그 목록 조회
     * @return 태그 목록 (tagId, name)
     */
    @Transactional(readOnly = true)
    public List<TagDto> getAllTags() {
        List<Tag> tags = tagRepository.findByEnabledTrue();
        
        return tags.stream()
                .map(tag -> TagDto.builder()
                        .tagId(tag.getId())
                        .name(tag.getName())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 태그 생성
     * @param name 태그 이름
     * @return 생성된 태그 ID
     */
    @Transactional
    public Long createTag(String name) {
        // 태그 이름 중복 검사
        if (tagRepository.existsByName(name)) {
            throw new RuntimeException("이미 존재하는 태그 이름입니다: " + name);
        }

        // 태그 생성
        Tag tag = Tag.builder()
                .name(name)
                .enabled(true)
                .build();

        Tag savedTag = tagRepository.save(tag);
        log.info("태그 생성 완료: tagId={}, name={}", savedTag.getId(), savedTag.getName());

        return savedTag.getId();
    }

    /**
     * 태그 수정
     * @param tagId 태그 ID
     * @param name 새로운 태그 이름
     * @return 수정된 태그 ID
     */
    @Transactional
    public Long updateTag(Long tagId, String name) {
        // 태그 조회
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("태그를 찾을 수 없습니다."));

        // 태그 이름 중복 검사 (이름이 변경되는 경우에만)
        if (!tag.getName().equals(name) && tagRepository.existsByName(name)) {
            throw new RuntimeException("이미 존재하는 태그 이름입니다: " + name);
        }

        // 태그 이름 수정
        tag.update(name);
        tagRepository.save(tag);

        log.info("태그 수정 완료: tagId={}, name={}", tagId, name);

        return tagId;
    }

    /**
     * 매거진에 태그 추가
     * @param magazineId 매거진 ID
     * @param tagId 태그 ID
     * @return 생성된 MagazineTag ID
     */
    @Transactional
    public Long addTagToMagazine(Long magazineId, Long tagId) {
        // 매거진 조회
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));
        
        // 태그 조회
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new RuntimeException("태그를 찾을 수 없습니다."));
        
        // 이미 매핑되어 있는지 확인
        if (magazineTagRepository.existsByMagazineIdAndTagId(magazineId, tagId)) {
            throw new RuntimeException("이미 해당 태그가 매거진에 추가되어 있습니다.");
        }
        
        // MagazineTag 생성
        MagazineTag magazineTag = MagazineTag.builder()
                .magazine(magazine)
                .tag(tag)
                .enabled(true)
                .build();
        
        MagazineTag savedMagazineTag = magazineTagRepository.save(magazineTag);
        log.info("매거진에 태그 추가 완료: magazineId={}, tagId={}, magazineTagId={}", 
                 magazineId, tagId, savedMagazineTag.getId());
        
        return savedMagazineTag.getId();
    }

    /**
     * 매거진에서 태그 삭제 (비활성화)
     * @param magazineId 매거진 ID
     * @param tagId 태그 ID
     * @return 삭제된 MagazineTag ID
     */
    @Transactional
    public Long removeTagFromMagazine(Long magazineId, Long tagId) {
        // MagazineTag 조회
        MagazineTag magazineTag = magazineTagRepository.findByMagazineIdAndTagId(magazineId, tagId)
                .orElseThrow(() -> new RuntimeException("매거진에 해당 태그가 추가되어 있지 않습니다."));
        
        // 태그 비활성화 (소프트 삭제)
        magazineTag.disable();
        magazineTagRepository.save(magazineTag);
        
        log.info("매거진에서 태그 삭제 완료: magazineId={}, tagId={}, magazineTagId={}", 
                 magazineId, tagId, magazineTag.getId());
        
        return magazineTag.getId();
    }

    /**
     * 매거진 카드에 장소 매핑 (또는 매핑 해제)
     * @param magazineId 매거진 ID
     * @param cardOrder 카드 순서
     * @param placeId 장소 ID (null이면 매핑 해제)
     * @return 매거진 카드 ID
     */
    @Transactional
    public Long mapPlaceToCard(Long magazineId, Integer cardOrder, Long placeId) {
        // 1. 매거진 카드 조회 (매거진 ID와 order로)
        MagazineCard card = magazineCardRepository.findByMagazineIdAndOrder(magazineId, cardOrder)
                .orElseThrow(() -> new RuntimeException("매거진 카드를 찾을 수 없습니다: magazineId=" + magazineId + ", order=" + cardOrder));

        // 2. 장소 매핑 또는 해제
        if (placeId != null) {
            // 장소 조회 및 유효성 검사
            Place place = placeRepository.findById(placeId)
                    .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다: " + placeId));
            
            // 장소가 활성화되어 있는지 확인
            if (!place.isEnabled()) {
                throw new RuntimeException("비활성화된 장소는 매핑할 수 없습니다: " + placeId);
            }
            
            // 장소 매핑
            card.updatePlace(place);
            log.info("매거진 카드에 장소 매핑 완료: magazineId={}, cardOrder={}, placeId={}", magazineId, cardOrder, placeId);
        } else {
            // 매핑 해제
            card.updatePlace(null);
            log.info("매거진 카드에서 장소 매핑 해제 완료: magazineId={}, cardOrder={}", magazineId, cardOrder);
        }

        return card.getId();
    }

    /**
     * MagazineCard를 MagazineCardDto로 변환
     */
    private MagazineCardDto convertToCardDto(MagazineCard card) {
        return MagazineCardDto.builder()
                .order(card.getOrder())
                .cardUrl(card.getCardUrl())
                .build();
    }
}

