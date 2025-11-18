package sandri.sandriweb.domain.point.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 포인트 적립 조건 타입
 */
@Getter
@RequiredArgsConstructor
public enum ConditionType {

    SIGN_UP("회원가입", "신규 회원가입 시 포인트 적립"),
    REFERRAL("친구 초대", "추천인 포인트 적립"),
    PLACE_VISIT("방문 포인트", "관광지 방문 시 포인트 적립"),
    REVIEW_CREATE("리뷰 포인트", "리뷰 작성 시 포인트 적립"),
    REVIEW_WITH_PHOTO("사진 리뷰 포인트", "사진이 포함된 리뷰 작성 시 포인트 적립"),
    PROFILE_COMPLETE("프로필 완성", "프로필 정보 완성 시 포인트 적립");

    private final String title;
    private final String description;
}
