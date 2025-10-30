package sandri.sandriweb.domain.place.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sandri.sandriweb.domain.place.entity.Place;

interface MemberRepository extends JpaRepository<Place, Long> {


}
