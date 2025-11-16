package sandri.sandriweb.domain.favorite.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.favorite.dto.FavoriteListResponseDto;
import sandri.sandriweb.domain.favorite.dto.FavoriteRouteDto;
import sandri.sandriweb.domain.magazine.dto.MagazineListDto;
import sandri.sandriweb.domain.magazine.service.MagazineService;
import sandri.sandriweb.domain.place.dto.SimplePlaceDto;
import sandri.sandriweb.domain.place.service.PlaceService;
import sandri.sandriweb.domain.route.service.RouteService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private final PlaceService placeService;
    private final RouteService routeService;
    private final MagazineService magazineService;

    public FavoriteListResponseDto getFavoriteList(Long userId) {
        List<SimplePlaceDto> places = placeService.getLikedPlaces(userId);
        List<FavoriteRouteDto> routes = routeService.getLikedRoutes(userId);
        List<MagazineListDto> magazines = magazineService.getLikedMagazines(userId);

        return FavoriteListResponseDto.builder()
                .places(places)
                .routes(routes)
                .magazines(magazines)
                .build();
    }
}

