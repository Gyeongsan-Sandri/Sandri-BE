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
        자연_힐링("자연/힐링"),
        역사_전통("역사/전통"),
        문화_체험("문화/체험"),
        식도락("식도락"),
        액티비티("액티비티"),
        쇼핑("쇼핑"),
        휴양("휴양"),
        도시탐방("도시 탐방");
        
        private final String displayName;
        
        TravelStyle(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
