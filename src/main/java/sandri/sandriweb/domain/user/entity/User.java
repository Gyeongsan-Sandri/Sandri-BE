package sandri.sandriweb.domain.user.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import sandri.sandriweb.global.entity.BaseEntity;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class User extends BaseEntity implements UserDetails {
    
    @Id
    @Column(name = "user_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 50)
    private String name;
    
    @Column(nullable = false)
    private LocalDate birthDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;
    
    @Column(nullable = false, length = 100)
    private String location;
    
    @Column(length = 30)
    private String referrerUsername;

    @Column(nullable = false, unique = true, length = 30)
    private String nickname;
    
    @Column(nullable = false, unique = true, length = 30)
    private String username;
    
    @Column(nullable = false)
    private String password;
    
    @Column(nullable = false)
    private boolean phoneVerified;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TravelStyle travelStyle;
    
    @Column
    private Double latitude;
    
    @Column
    private Double longitude;

    @Column(nullable = false)
    @lombok.Builder.Default
    private Long point = 0L;
    
    // UserDetails 구현
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
    
    @Override
    public String getPassword() {
        return password;
    }
    
    @Override
    public String getUsername() {
        return username;
    }
    
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    
    public void updatePassword(String password) {
        this.password = password;
    }
    
    public void verifyPhone() {
        this.phoneVerified = true;
    }
    
    public void updateLocation(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    public void updateTravelStyle(TravelStyle travelStyle) {
        this.travelStyle = travelStyle;
    }
    
    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    public void addPoint(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("포인트 증감은 음수일 수 없습니다");
        }
        if (this.point == null) {
            this.point = 0L;
        }
        this.point += amount;
    }

    public void subtractPoint(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("포인트 증감은 음수일 수 없습니다");
        }
        if (this.point == null) {
            this.point = 0L;
        }
        if (this.point < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다");
        }
        this.point -= amount;
    }

    @PrePersist
    @PostLoad
    private void ensurePointInitialized() {
        if (this.point == null) {
            this.point = 0L;
        }
    }
    
    @Schema(description = "성별", allowableValues = {"MALE", "FEMALE", "OTHER"})
    public enum Gender {
        MALE, FEMALE, OTHER
    }
    
    @Schema(
            description = "여행 스타일 유형: ① 모험왕(실외+빡빡+로컬), ② 감성요정(실내+여유+감성), ③ 핫플 헌터(실외+빡빡+감성), ④ 현지인(실외+여유+로컬), ⑤ 철저 플래너(실내+빡빡+로컬), ⑥ 힐링 거북이(실내+여유+로컬), ⑦ 산책가(실외+여유+감성), ⑧ 갤러리피플(실내+빡빡+감성)",
            example = "ADVENTURER"
    )
    public enum TravelStyle {
        ADVENTURER,         // ① 모험왕 (실외 + 빡빡 + 로컬)
        SENSITIVE_FAIRY,    // ② 감성요정 (실내 + 여유 + 감성)
        HOTSPOT_HUNTER,     // ③ 핫플 헌터 (실외 + 빡빡 + 감성)
        LOCAL,              // ④ 현지인 (실외 + 여유 + 로컬)
        THOROUGH_PLANNER,   // ⑤ 철저 플래너 (실내 + 빡빡 + 로컬)
        HEALING_TURTLE,     // ⑥ 힐링 거북이 (실내 + 여유 + 로컬)
        WALKER,             // ⑦ 산책가 (실외 + 여유 + 감성)
        GALLERY_PEOPLE      // ⑧ 갤러리피플 (실내 + 빡빡 + 감성)
    }
}
