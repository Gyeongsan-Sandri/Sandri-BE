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
import sandri.sandriweb.domain.magazine.entity.mapping.UserMagazine;
import sandri.sandriweb.domain.magazine.repository.MagazineCardRepository;
import sandri.sandriweb.domain.magazine.repository.MagazineRepository;
import sandri.sandriweb.domain.magazine.repository.UserMagazineRepository;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.domain.user.repository.UserRepository;
import sandri.sandriweb.global.entity.BaseEntity;

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

    /*
     * 매거진 상세 조회 (카드뉴스 포함)
     * @param magazineId 매거진 ID
     * @return 매거진 상세 정보와 카드뉴스 리스트
     */
    @Transactional(readOnly = true)
    public MagazineDetailResponseDto getMagazineDetail(Long magazineId) {
        // 1. 매거진과 카드를 함께 조회 (FETCH JOIN)
        Magazine magazine = magazineRepository.findByIdWithCards(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));

        // 2. MagazineCard를 DTO로 변환 (DB에서 이미 enabled 필터링 및 order 정렬됨)
        List<MagazineCardDto> cardDtos = new ArrayList<>();
        if (magazine.getCards() != null && !magazine.getCards().isEmpty()) {
            cardDtos = magazine.getCards().stream()
                    .map(this::convertToCardDto)
                    .collect(Collectors.toList());
        }

        log.info("매거진 상세 조회: magazineId={}, cardCount={}, totalCardsFetched={}", 
                 magazineId, cardDtos.size(), 
                 magazine.getCards() != null ? magazine.getCards().size() : 0);

        // DTO 생성 및 반환
        return MagazineDetailResponseDto.builder()
                .magazineId(magazine.getId())
                .name(magazine.getName())
                .summary(magazine.getSummary())
                .content(magazine.getContent())
                .cards(cardDtos)
                .build();
    }

    /*
     * 커서 기반 매거진 목록 조회 (썸네일만 포함)
     * @param lastMagazineId 마지막으로 조회한 매거진 ID (첫 조회시 null)
     * @param size 페이지 크기
     * @param userId 사용자 ID (로그인한 경우에만 제공, null 가능)
     * @return 매거진 목록 (제목, 썸네일, 요약, 좋아요 여부)
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

        // 3. DTO 변환
        List<MagazineListDto> content = pageItems.stream()
                .map(magazine -> {
                    // 썸네일 가져오기 (order = 0인 카드만 fetch join되어 있음)
                    String thumbnail = null;
                    if (magazine.getCards() != null && !magazine.getCards().isEmpty()) {
                        // fetch join으로 가져온 카드는 order = 0이고 enabled = true인 카드만 존재
                        MagazineCard thumbnailCard = magazine.getCards().get(0);
                        thumbnail = thumbnailCard != null ? thumbnailCard.getCardUrl() : null;
                    }

                    // 사용자가 좋아요한 매거진인지 확인
                    Boolean isLiked = userId != null ? likedMagazineIds.getOrDefault(magazine.getId(), false) : null;

                    return MagazineListDto.builder()
                            .magazineId(magazine.getId())
                            .title(magazine.getName())
                            .thumbnail(thumbnail)
                            .summary(magazine.getSummary())
                            .isLiked(isLiked)
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
        // 매거진 존재 확인 (exists로 최적화 가능하지만, 이후 매거진 객체가 필요할 수 있어서 유지)
        if (!magazineRepository.existsById(magazineId)) {
            throw new RuntimeException("매거진을 찾을 수 없습니다.");
        }
        
        // 사용자 존재 확인 (exists로 최적화)
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

    // createMagazineCards, updateMagazineCards 헬퍼 메소드    
    private List<MagazineCard> createMagazineCards(Magazine magazine, List<String> cardUrls) {
        if (cardUrls == null || cardUrls.isEmpty()) {
            return List.of(); // Java 9+에서는 List.of() 사용 권장
        }

        List<MagazineCard> cards = new ArrayList<>();
        for (int i = 0; i < cardUrls.size(); i++) {
            MagazineCard card = MagazineCard.builder()
                    .magazine(magazine)
                    .cardUrl(cardUrls.get(i))
                    .order(i)
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
        // 1. 매거진 생성
        Magazine magazine = Magazine.builder()
                .name(request.getName())
                .summary(request.getSummary())
                .content(request.getContent())
                .build();

        Magazine savedMagazine = magazineRepository.save(magazine);

        // 2. 매거진 카드 생성 (순서대로)
        List<MagazineCard> cards = createMagazineCards(savedMagazine, request.getCardUrls());
        if (!cards.isEmpty()) {
            magazineCardRepository.saveAll(cards);
            log.info("매거진 카드 {}개 추가 완료: magazineId={}", cards.size(), savedMagazine.getId());
        }

        log.info("매거진 생성 완료: magazineId={}, name={}, cardCount={}",
                 savedMagazine.getId(), savedMagazine.getName(),
                 request.getCardUrls() != null ? request.getCardUrls().size() : 0);

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
        // 매거진 조회
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));

        // 1. 매거진 정보 수정
        magazine.update(request.getName(), request.getSummary(), request.getContent());

        // 2. 기존 카드 제거 및 새로운 카드 추가 (orphanRemoval로 자동 삭제)
        magazine.getCards().clear();
        
        // 3. 새로운 카드 생성 및 추가
        List<MagazineCard> newCards = createMagazineCards(magazine, request.getCardUrls());
        if (!newCards.isEmpty()) {
            magazine.getCards().addAll(newCards);
            log.info("매거진 카드 {}개 교체 완료 (기존 카드는 자동 삭제됨): magazineId={}", 
                     newCards.size(), magazineId);
        }

        log.info("매거진 수정 완료: magazineId={}, name={}, cardCount={}",
                 magazine.getId(), magazine.getName(),
                 request.getCardUrls() != null ? request.getCardUrls().size() : 0);

        return magazine.getId();
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

