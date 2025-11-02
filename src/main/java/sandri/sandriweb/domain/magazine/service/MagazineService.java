package sandri.sandriweb.domain.magazine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import sandri.sandriweb.domain.magazine.dto.MagazineCardDto;
import sandri.sandriweb.domain.magazine.dto.MagazineDetailResponseDto;
import sandri.sandriweb.domain.magazine.dto.MagazineListDto;
import sandri.sandriweb.domain.magazine.entity.Magazine;
import sandri.sandriweb.domain.magazine.entity.MagazineCard;
import sandri.sandriweb.domain.magazine.repository.MagazineRepository;
import sandri.sandriweb.domain.magazine.repository.UserMagazineRepository;

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
    private final UserMagazineRepository userMagazineRepository;

    /**
     * 매거진 상세 조회 (카드뉴스 포함)
     * @param magazineId 매거진 ID
     * @return 매거진 상세 정보와 카드뉴스 리스트
     */
    @Transactional(readOnly = true)
    public MagazineDetailResponseDto getMagazineDetail(Long magazineId) {
        // Magazine과 MagazineCard를 함께 조회 (FETCH JOIN)
        Magazine magazine = magazineRepository.findByIdWithCards(magazineId)
                .orElseThrow(() -> new RuntimeException("매거진을 찾을 수 없습니다."));

        // MagazineCard를 DTO로 변환 (enabled된 카드만)
        List<MagazineCardDto> cardDtos = magazine.getCards().stream()
                .filter(card -> card.isEnabled()) // enabled된 카드만 필터링
                .map(this::convertToCardDto)
                .collect(Collectors.toList());

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
                .map(id -> magazineRepository.findByIdWithCards(id))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());

        // 사용자가 좋아요한 매거진 ID 조회 (로그인한 경우)
        Map<Long, Boolean> likedMagazineIds = new HashMap<>();
        if (userId != null) {
            List<Long> likedIds = userMagazineRepository.findLikedMagazineIdsByUserId(userId, magazineIds);
            likedMagazineIds = likedIds.stream()
                    .collect(Collectors.toMap(
                            magazineId -> magazineId,
                            magazineId -> true
                    ));
        }

        // DTO 변환
        return magazinesWithCards.stream()
                .map(magazine -> {
                    // 첫 번째 enabled된 카드 찾기
                    MagazineCard firstCard = magazine.getCards().stream()
                            .filter(MagazineCard::isEnabled)
                            .sorted((c1, c2) -> c1.getCreatedAt().compareTo(c2.getCreatedAt()))
                            .findFirst()
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
     * MagazineCard를 MagazineCardDto로 변환
     */
    private MagazineCardDto convertToCardDto(MagazineCard card) {
        return MagazineCardDto.builder()
                .cardId(card.getId())
                .cardUrl(card.getCardUrl())
                .build();
    }
}

