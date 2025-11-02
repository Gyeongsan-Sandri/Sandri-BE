package sandri.sandriweb.domain.advertise.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sandri.sandriweb.domain.advertise.dto.AdDto;
import sandri.sandriweb.domain.advertise.dto.AdResponseDto;
import sandri.sandriweb.domain.advertise.entity.OfficialAd;
import sandri.sandriweb.domain.advertise.entity.PrivateAd;
import sandri.sandriweb.domain.advertise.repository.OfficialAdRepository;
import sandri.sandriweb.domain.advertise.repository.PrivateAdRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdvertiseService {

    private final OfficialAdRepository officialAdRepository;
    private final PrivateAdRepository privateAdRepository;

    /**
     * 유효한 공식 광고 조회
     * @return 전체 배너 개수와 광고 리스트
     */
    @Transactional(readOnly = true)
    public AdResponseDto getOfficialAds() {
        LocalDateTime now = LocalDateTime.now();
        List<OfficialAd> validAds = officialAdRepository.findValidAds(now);

        List<AdDto> adDtos = validAds.stream()
                .map(this::convertToAdDto)
                .collect(Collectors.toList());

        return AdResponseDto.builder()
                .totalCount(adDtos.size())
                .ads(adDtos)
                .build();
    }

    /**
     * 유효한 개인 광고 조회
     * @return 전체 배너 개수와 광고 리스트
     */
    @Transactional(readOnly = true)
    public AdResponseDto getPrivateAds() {
        LocalDateTime now = LocalDateTime.now();
        List<PrivateAd> validAds = privateAdRepository.findValidAds(now);

        List<AdDto> adDtos = validAds.stream()
                .map(this::convertToAdDto)
                .collect(Collectors.toList());

        return AdResponseDto.builder()
                .totalCount(adDtos.size())
                .ads(adDtos)
                .build();
    }

    /**
     * OfficialAd를 AdDto로 변환
     */
    private AdDto convertToAdDto(OfficialAd ad) {
        return AdDto.builder()
                .adId(ad.getId())
                .imageUrl(ad.getImageUrl())
                .title(ad.getTitle())
                .description(ad.getDescription())
                .linkUrl(ad.getLinkUrl())
                .build();
    }

    /**
     * PrivateAd를 AdDto로 변환
     */
    private AdDto convertToAdDto(PrivateAd ad) {
        return AdDto.builder()
                .adId(ad.getId())
                .imageUrl(ad.getImageUrl())
                .title(ad.getTitle())
                .description(ad.getDescription())
                .linkUrl(ad.getLinkUrl())
                .build();
    }
}

