package sandri.sandriweb.domain.user.entity;

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
    
    public void updateTravelStyle(TravelStyle travelStyle) {
        this.travelStyle = travelStyle;
    }
    
    public enum Gender {
        MALE, FEMALE, OTHER
    }
    
    public enum TravelStyle {
        모험왕("모험왕", "실외 + 빡빡 + 로컬", "쉬는 건 집에서! 여행은 발이 부르트도록 해야 제맛"),
        감성요정("감성요정", "실내 + 여유 + 감성", "내 여행 앨범은 곧 작품집"),
        핫플_헌터("핫플 헌터", "실외 + 빡빡 + 감성", "유명 포토스팟, 공연, 핫플을 빠짐없이 방문"),
        현지인("현지인", "실외 + 여유 + 로컬", "관광지도 좋지만 진짜는 골목길에 있다"),
        철저_플래너("철저 플래너", "실내 + 빡빡 + 로컬", "여행은 준비 70%, 실행 30%다"),
        힐링_거북이("힐링 거북이", "실내 + 여유 + 로컬", "여행도 결국은 힐링이 우선이지"),
        산책가("산책가", "실외 + 여유 + 감성", "시간은 느리게, 감성은 깊게"),
        갤러리피플("갤러리피플", "실내 + 빡빡 + 감성", "내 일정은 곧 아트 전시회");
        
        private final String displayName;
        private final String description;
        private final String tagline;
        
        TravelStyle(String displayName, String description, String tagline) {
            this.displayName = displayName;
            this.description = description;
            this.tagline = tagline;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
        
        public String getTagline() {
            return tagline;
        }
    }
}
