package sandri.sandriweb.domain.dataImport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GbgsTourApiResponse {

    @JsonProperty("item")
    private List<TourItem> item;

    @JsonProperty("pageNo")
    private Integer pageNo;

    @JsonProperty("numOfRows")
    private Integer numOfRows;

    @JsonProperty("totalCount")
    private Integer totalCount;

    @JsonProperty("resultCode")
    private String resultCode;

    @JsonProperty("resultMsg")
    private String resultMsg;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TourItem {
        @JsonProperty("title")
        private String title; // 장소명

        @JsonProperty("address")
        private String address; // 주소

        @JsonProperty("latitude")
        private String latitude; // 위도

        @JsonProperty("longitude")
        private String longitude; // 경도

        @JsonProperty("contents")
        private String contents; // 내용

        @JsonProperty("summary")
        private String summary; // 요약

        @JsonProperty("phone")
        private String phone; // 전화번호

        @JsonProperty("homepage")
        private String homepage; // 홈페이지

        @JsonProperty("image")
        private String image; // 이미지 URL

        @JsonProperty("imageUrl")
        private String imageUrl; // 이미지 URL (대체 필드명)
    }
}
