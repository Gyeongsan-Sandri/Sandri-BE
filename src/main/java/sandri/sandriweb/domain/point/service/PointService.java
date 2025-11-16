package sandri.sandriweb.domain.point.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.point.dto.ExpiringPointsResponseDto;
import sandri.sandriweb.domain.point.dto.PointHistoryDto;
import sandri.sandriweb.domain.point.dto.PointHistoryResponseDto;
import sandri.sandriweb.domain.point.entity.PointEarnCondition;
import sandri.sandriweb.domain.point.entity.PointHistory;
import sandri.sandriweb.domain.point.enums.ConditionType;
import sandri.sandriweb.domain.point.enums.PointHistoryType;
import sandri.sandriweb.domain.point.repository.PointEarnConditionRepository;
import sandri.sandriweb.domain.point.repository.PointHistoryRepository;
import sandri.sandriweb.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PointService {

    private final PointEarnConditionRepository pointEarnConditionRepository;
    private final PointHistoryRepository pointHistoryRepository;

    /**
     * 포인트 적립 조건 등록 또는 수정
     * 조건 타입이 이미 존재하면 포인트 양을 업데이트하고,
     * 존재하지 않으면 새로 생성합니다.
     *
     * @param conditionType 적립 조건 타입
     * @param pointAmount 적립 포인트 양
     * @return 생성 또는 수정된 적립 조건 ID
     */
    @Transactional
    public Long createOrUpdateEarnCondition(ConditionType conditionType, Long pointAmount) {
        log.info("포인트 적립 조건 등록/수정: conditionType={}, pointAmount={}", conditionType, pointAmount);

        // 기존 조건 조회
        Optional<PointEarnCondition> existingCondition =
                pointEarnConditionRepository.findByConditionType(conditionType);

        if (existingCondition.isPresent()) {
            // 이미 존재하면 포인트 양 업데이트
            PointEarnCondition condition = existingCondition.get();
            condition.updatePointAmount(pointAmount);
            log.info("기존 포인트 적립 조건 업데이트 완료: id={}, conditionType={}",
                    condition.getId(), conditionType);
            return condition.getId();
        } else {
            // 존재하지 않으면 새로 생성
            PointEarnCondition newCondition = PointEarnCondition.builder()
                    .conditionType(conditionType)
                    .pointAmount(pointAmount)
                    .build();

            PointEarnCondition saved = pointEarnConditionRepository.save(newCondition);
            log.info("새로운 포인트 적립 조건 생성 완료: id={}, conditionType={}",
                    saved.getId(), conditionType);
            return saved.getId();
        }
    }

    /**
     * 사용자의 포인트 히스토리 목록 조회 (타입별 필터링)
     * @param user 사용자 엔티티 (Controller에서 전달)
     * @param type 조회 타입 (ALL: 전체, EARN: 적립만, USE: 사용만)
     * @return 포인트 히스토리 목록
     */
    public List<PointHistoryDto> getUserPointHistoryList(User user, PointHistoryType type) {
        // 사용자 검증
        if (user == null) {
            throw new RuntimeException("사용자 정보가 없습니다");
        }

        log.info("포인트 히스토리 목록 조회: userId={}, type={}", user.getId(), type);

        // 사용자의 모든 포인트 히스토리 조회 (최신순)
        List<PointHistory> histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // DTO 리스트로 변환 및 타입별 필터링
        List<PointHistoryDto> historyDtos = convertToHistoryDtos(histories, type);

        log.info("포인트 히스토리 목록 조회 완료: userId={}, type={}, historyCount={}",
                user.getId(), type, historyDtos.size());

        return historyDtos;
    }

    /**
     * 포인트 히스토리 목록과 소멸 예정 포인트를 한 번에 조회 (중복 쿼리 방지)
     * @param user 사용자 엔티티 (Controller에서 전달)
     * @param type 조회 타입 (ALL: 전체, EARN: 적립만, USE: 사용만)
     * @return 포인트 히스토리 응답 DTO (히스토리 목록 + 소멸 예정 포인트)
     */
    public PointHistoryResponseDto getUserPointHistoryWithExpiring(User user, PointHistoryType type) {
        // 사용자 검증
        if (user == null) {
            throw new RuntimeException("사용자 정보가 없습니다");
        }

        log.info("포인트 히스토리 및 소멸 예정 포인트 조회: userId={}, type={}", user.getId(), type);

        // 사용자의 모든 포인트 히스토리 조회 (최신순) - 한 번만 조회
        List<PointHistory> histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // DTO 리스트로 변환 및 타입별 필터링
        List<PointHistoryDto> historyDtos = convertToHistoryDtos(histories, type);

        // 소멸 예정 포인트 계산 (동일한 histories 재사용)
        Long expiringPoints = calculateExpiringPointsWithin7Days(histories);

        log.info("포인트 히스토리 및 소멸 예정 포인트 조회 완료: userId={}, type={}, historyCount={}, expiringPoints={}",
                user.getId(), type, historyDtos.size(), expiringPoints);

        return PointHistoryResponseDto.builder()
                .expiringPointsWithin7Days(expiringPoints)
                .histories(historyDtos)
                .build();
    }

    /**
     * 포인트 히스토리 리스트를 DTO로 변환 (공통 메소드)
     * @param histories 포인트 히스토리 리스트
     * @param type 조회 타입
     * @return DTO 리스트
     */
    private List<PointHistoryDto> convertToHistoryDtos(List<PointHistory> histories, PointHistoryType type) {
        return histories.stream()
                .filter(PointHistory::isEnabled) // enabled된 것만
                .filter(history -> filterByType(history, type)) // 타입별 필터링
                .map(history -> PointHistoryDto.builder()
                        .createdAt(history.getCreatedAt().toLocalDate())
                        .conditionTypeTitle(history.getConditionType() != null
                                ? history.getConditionType().getTitle()
                                : "포인트 사용")
                        .pointAmount(history.getPointAmount())
                        .balanceAfter(history.getBalanceAfter())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 포인트 히스토리 타입별 필터링
     * @param history 포인트 히스토리
     * @param type 조회 타입
     * @return 필터링 결과 (true: 포함, false: 제외)
     */
    private boolean filterByType(PointHistory history, PointHistoryType type) {
        return switch (type) {
            case ALL -> true; // 전체 조회
            case EARN -> history.getPointAmount() > 0; // 적립만 (양수)
            case USE -> history.getPointAmount() < 0; // 사용만 (음수)
        };
    }

    /**
     * 포인트 적립 (공통 메소드)
     * @param user 사용자 엔티티
     * @param conditionType 적립 조건 타입
     */
    @Transactional
    public void earnPoints(User user, ConditionType conditionType) {
        log.info("포인트 적립 시작: userId={}, conditionType={}", user.getId(), conditionType);

        try {
            // 해당 조건의 포인트 양 조회
            PointEarnCondition earnCondition = pointEarnConditionRepository
                    .findByConditionType(conditionType)
                    .orElse(null);

            if (earnCondition != null) {
                Long pointAmount = earnCondition.getPointAmount();

                // 사용자 포인트 증가
                user.addPoint(pointAmount);

                // 포인트 히스토리 저장
                PointHistory pointHistory = PointHistory.builder()
                        .user(user)
                        .conditionType(conditionType)
                        .pointAmount(pointAmount)
                        .balanceAfter(user.getPoint())
                        .build();

                pointHistoryRepository.save(pointHistory);
                log.info("포인트 적립 완료: userId={}, conditionType={}, pointAmount={}, balanceAfter={}",
                        user.getId(), conditionType, pointAmount, user.getPoint());
            } else {
                log.warn("{} 포인트 적립 조건이 설정되지 않았습니다.", conditionType);
            }
        } catch (Exception e) {
            log.error("포인트 적립 중 오류 발생: userId={}, conditionType={}, error={}",
                    user.getId(), conditionType, e.getMessage(), e);
            throw e; // 예외를 다시 던져서 호출자가 처리하도록 함
        }
    }

    /**
     * 7일 이내 소멸 예정 포인트 조회
     * @param user 사용자 엔티티 (Controller에서 전달)
     * @return 소멸 예정 포인트 DTO
     */
    public ExpiringPointsResponseDto getExpiringPoints(User user) {
        // 사용자 검증
        if (user == null) {
            throw new RuntimeException("사용자 정보가 없습니다");
        }

        log.info("소멸 예정 포인트 조회: userId={}", user.getId());

        // 사용자의 모든 포인트 히스토리 조회
        List<PointHistory> histories = pointHistoryRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        // 7일 이내 소멸 예정 포인트 계산
        Long expiringPoints = calculateExpiringPointsWithin7Days(histories);

        log.info("소멸 예정 포인트 조회 완료: userId={}, expiringPoints={}", user.getId(), expiringPoints);

        return ExpiringPointsResponseDto.builder()
                .expiringPointsWithin7Days(expiringPoints)
                .build();
    }

    /**
     * 7일 이내 소멸 예정 포인트 계산
     * 포인트는 적립 후 30일이 지나면 소멸됨
     * 7일 이내 소멸 예정 = 적립일(updatedAt)이 23~30일 전 사이인 포인트
     *
     * @param histories 포인트 히스토리 리스트
     * @return 7일 이내 소멸 예정 포인트 합계
     */
    private Long calculateExpiringPointsWithin7Days(List<PointHistory> histories) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime twentyThreeDaysAgo = now.minusDays(23);

        return histories.stream()
                .filter(PointHistory::isEnabled) // enabled = true
                .filter(h -> h.getPointAmount() > 0) // 적립(양수)만
                .filter(h -> h.getUpdatedAt() != null) // updatedAt이 null이 아닌 것
                .filter(h -> !h.getUpdatedAt().isBefore(thirtyDaysAgo)) // updatedAt >= 30일 전
                .filter(h -> h.getUpdatedAt().isBefore(twentyThreeDaysAgo)) // updatedAt < 23일 전
                .mapToLong(PointHistory::getPointAmount)
                .sum();
    }
}
