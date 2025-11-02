package sandri.sandriweb.domain.place.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.user.entity.User;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private Long userId;
    private String nickname;
    private String profileImageUrl; // User 엔티티에 프로필 이미지 필드가 있다고 가정

    public static UserProfileDto from(User user) {
        return UserProfileDto.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .profileImageUrl(null) // TODO: User 엔티티에 profileImageUrl 필드가 추가되면 매핑
                .build();
    }
}

