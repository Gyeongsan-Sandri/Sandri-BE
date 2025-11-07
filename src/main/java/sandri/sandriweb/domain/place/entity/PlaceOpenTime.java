package sandri.sandriweb.domain.place.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import sandri.sandriweb.global.entity.BaseEntity;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "place_open_times")
public class PlaceOpenTime extends BaseEntity {

    @Id
    @Column(name = "place_open_time_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek dayOfWeek; // 요일 (MONDAY, TUESDAY, ...)

    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private boolean isOpen = true; // 영업일 여부

    @Column(name = "open_time")
    private LocalTime openTime; // 오픈 시간 (24시간 운영 시 null 가능)

    @Column(name = "close_time")
    private LocalTime closeTime; // 마감 시간 (24시간 운영 시 null 가능)

    @Column(name = "break_start_time")
    private LocalTime breakStartTime; // 휴게 시간 시작 (선택사항)

    @Column(name = "break_end_time")
    private LocalTime breakEndTime; // 휴게 시간 종료 (선택사항)
}

