package sandri.sandriweb.domain.advertise.entity;

/**
 * 공식 광고와 개인 광고의 공통 인터페이스
 */
public interface Ad {
    Long getId();
    String getImageUrl();
    String getTitle();
    String getDescription();
    String getLinkUrl();
}

