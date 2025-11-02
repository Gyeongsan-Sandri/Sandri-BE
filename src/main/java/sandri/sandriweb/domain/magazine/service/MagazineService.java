package sandri.sandriweb.domain.magazine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import sandri.sandriweb.domain.magazine.dto.CreateMagazineRequestDto;
import sandri.sandriweb.domain.magazine.dto.MagazineCardDto;
import sandri.sandriweb.domain.magazine.dto.MagazineDetailResponseDto;
import sandri.sandriweb.domain.magazine.dto.MagazineListDto;
import sandri.sandriweb.domain.magazine.dto.UpdateMagazineRequestDto;
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

    /**
     * 매거진 상세 조회 (카드뉴스 포함)
     * @param magazineId 매거진 ID
     * @return 매거진 상세 정보와 카드뉴스 리스트
     */
    @Transactional(readOnly = true)
    public MagazineDetailResponseDto getMagazineDetail(Long magazineId) {
        // 1. 매거진과 카드를 함께 조회 (FETCH JOIN)
        Magazine magazine = magazineRepository.findByIdWithCards(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));

        // 2. MagazineCard를 DTO로 변환 (enabled된 카드만, createdAt 순으로 정렬)
        List<MagazineCardDto> cardDtos = new ArrayList<>();
        if (magazine.getCards() != null && !magazine.getCards().isEmpty()) {
            cardDtos = magazine.getCards().stream()
                    .filter(BaseEntity::isEnabled) // enabled된 카드만 필터링
                    .sorted((c1, c2) -> {
                        if (c1.getCreatedAt() == null && c2.getCreatedAt() == null) return 0;
                        if (c1.getCreatedAt() == null) return 1;
                        if (c2.getCreatedAt() == null) return -1;
                        return c1.getCreatedAt().compareTo(c2.getCreatedAt()); // 오름차순 정렬
                    })
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

    /**
     * 매거진 목록 조회
     * @param count 조회할 개수
     * @param userId 사용자 ID (로그인한 경우에만 제공, null 가능)
     * @return 매거진 목록 (제목, 썸네일, 요약, 좋아요 여부)
     */
    @Transactional(readOnly = true)
    public List<MagazineListDto> getMagazineList(int count, Long userId) {
        // enabled된 매거진 조회 (최신순)
        Pageable pageable = PageRequest.of(0, count);
        List<Magazine> magazines = magazineRepository.findEnabledMagazinesOrderByCreatedAtDesc(pageable);

        // 각 매거진의 첫 번째 카드 조회를 위해 FETCH JOIN 필요
        List<Long> magazineIds = magazines.stream()
                .map(Magazine::getId)
                .collect(Collectors.toList());

        // 매거진과 첫 번째 카드를 함께 조회
        List<Magazine> magazinesWithCards = magazineIds.stream()
                .map(magazineRepository::findByIdWithCards)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();

        // 사용자가 좋아요한 매거진 ID 조회 (로그인한 경우)
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

        // DTO 변환
        return magazinesWithCards.stream()
                .map(magazine -> {
                    // 첫 번째 enabled된 카드 찾기
                    MagazineCard firstCard = magazine.getCards().stream()
                            .filter(MagazineCard::isEnabled).min((c1, c2) -> c1.getCreatedAt().compareTo(c2.getCreatedAt()))
                            .orElse(null);

                    String thumbnail = firstCard != null ? firstCard.getCardUrl() : null;

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
    }

    /**
     * 매거진 좋아요 토글
     * @param magazineId 매거진 ID
     * @param userId 사용자 ID
     * @return 좋아요 상태 (true: 좋아요 활성화, false: 좋아요 비활성화)
     */
    @Transactional
    public boolean toggleLike(Long magazineId, Long userId) {
        // 매거진 존재 확인
        Magazine magazine = magazineRepository.findById(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        
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
                    // 좋아요가 없는 경우: 새로 생성
                    UserMagazine newUserMagazine = UserMagazine.builder()
                            .user(user)
                            .magazine(magazine)
                            .build();
                    userMagazineRepository.save(newUserMagazine);
                    return true;
                });
    }

    /**
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
        if (request.getCardUrls() != null && !request.getCardUrls().isEmpty()) {
            log.info("매거진 카드 생성 시작: magazineId={}, cardUrlsCount={}", 
                     savedMagazine.getId(), request.getCardUrls().size());
            
            List<MagazineCard> cards = request.getCardUrls().stream()
                    .map(cardUrl -> MagazineCard.builder()
                            .magazine(savedMagazine)
                            .cardUrl(cardUrl)
                            .build())
                    .collect(Collectors.toList());

            List<MagazineCard> savedCards = magazineCardRepository.saveAll(cards);
            log.info("매거진 카드 {}개 추가 완료: magazineId={}, savedCardsCount={}", 
                     cards.size(), savedMagazine.getId(), savedCards.size());
            
            // 저장 직후 확인
            List<MagazineCard> verifyCards = magazineCardRepository.findByMagazineId(savedMagazine.getId());
            log.info("매거진 카드 저장 확인: magazineId={}, verifiedCount={}", 
                     savedMagazine.getId(), verifyCards.size());
        } else {
            log.info("매거진 카드 없음: magazineId={}", savedMagazine.getId());
        }

        log.info("매거진 생성 완료: magazineId={}, name={}, cardCount={}", 
                 savedMagazine.getId(), savedMagazine.getName(), 
                 request.getCardUrls() != null ? request.getCardUrls().size() : 0);

        return savedMagazine.getId();
    }

    /**
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
        Magazine updatedMagazine = magazineRepository.save(magazine);

        // 2. 기존 카드 삭제
        List<MagazineCard> existingCards = magazineCardRepository.findByMagazineId(magazineId);
        if (!existingCards.isEmpty()) {
            magazineCardRepository.deleteAll(existingCards);
            log.info("기존 매거진 카드 {}개 삭제 완료: magazineId={}", existingCards.size(), magazineId);
        }

        // 3. 새로운 카드 생성 (순서대로)
        if (request.getCardUrls() != null && !request.getCardUrls().isEmpty()) {
            List<MagazineCard> newCards = request.getCardUrls().stream()
                    .map(cardUrl -> MagazineCard.builder()
                            .magazine(updatedMagazine)
                            .cardUrl(cardUrl)
                            .build())
                    .collect(Collectors.toList());

            magazineCardRepository.saveAll(newCards);
            log.info("매거진 카드 {}개 추가 완료: magazineId={}", newCards.size(), magazineId);
        }

        log.info("매거진 수정 완료: magazineId={}, name={}, cardCount={}", 
                 updatedMagazine.getId(), updatedMagazine.getName(), 
                 request.getCardUrls() != null ? request.getCardUrls().size() : 0);

        return updatedMagazine.getId();
    }

    /**
     * MagazineCard를 MagazineCardDto로 변환
     */
    private MagazineCardDto convertToCardDto(MagazineCard card) {
        return MagazineCardDto.builder()
                .cardId(card.getId())
                .cardUrl(card.getCardUrl())
                .build();
    }
}

