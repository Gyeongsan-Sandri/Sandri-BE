package sandri.sandriweb.domain.route.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "route_locations")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteLocation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;
    
    @Column(nullable = false)
    private Integer dayNumber;
    
    @Column(nullable = false, length = 100)
    private String name;
    
    @Column(length = 200)
    private String address;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private Integer displayOrder;
    
    public void updateLocation(String name, String address, BigDecimal latitude, BigDecimal longitude, String description) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description == null ? "" : description;
    }
    
    public void updateOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}

