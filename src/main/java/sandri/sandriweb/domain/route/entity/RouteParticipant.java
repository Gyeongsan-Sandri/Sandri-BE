package sandri.sandriweb.domain.route.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import sandri.sandriweb.domain.user.entity.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "route_participants")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteParticipant {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private LocalDateTime joinedAt;
    
    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
    
    public static RouteParticipant create(Route route, User user) {
        return RouteParticipant.builder()
                .route(route)
                .user(user)
                .joinedAt(LocalDateTime.now())
                .build();
    }
}

