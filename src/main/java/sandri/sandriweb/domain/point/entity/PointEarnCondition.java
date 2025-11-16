package sandri.sandriweb.domain.point.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.domain.point.enums.ConditionType;
import sandri.sandriweb.global.entity.BaseEntity;

/**
 * 포인트 적립 조건 엔티티
 * 각 조건별로 적립되는 포인트 양을 관리합니다.
 */
@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "point_earn_conditions")
public class PointEarnCondition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "point_earn_condition_id")
    private Long id;

    /**
     * 적립 조건 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false, unique = true)
    private ConditionType conditionType;

    /**
     * 적립 포인트 양
     */
    @Column(name = "point_amount", nullable = false)
    private Long pointAmount;

    /**
     * 포인트 양 업데이트
     * @param pointAmount 새로운 포인트 양
     */
    public void updatePointAmount(Long pointAmount) {
        if (pointAmount < 0) {
            throw new IllegalArgumentException("포인트 양은 0 이상이어야 합니다");
        }
        this.pointAmount = pointAmount;
    }
}
