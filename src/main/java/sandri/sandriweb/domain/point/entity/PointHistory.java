package sandri.sandriweb.domain.point.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.domain.point.enums.ConditionType;
import sandri.sandriweb.domain.user.entity.User;
import sandri.sandriweb.global.entity.BaseEntity;

/**
 * 포인트 적립/사용 히스토리 엔티티
 * 사용자의 포인트 적립 및 사용 내역을 기록합니다.
 */
@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "point_histories")
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_history_id")
    private Long id;

    /**
     * 포인트를 적립/사용한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 적립 조건 (사용일 경우 null)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type")
    private ConditionType conditionType;

    /**
     * 포인트 변동량 (적립: 양수, 사용: 음수)
     */
    @Column(name = "point_amount", nullable = false)
    private Long pointAmount;

    /**
     * 포인트 변동 후 잔액
     */
    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;
}
