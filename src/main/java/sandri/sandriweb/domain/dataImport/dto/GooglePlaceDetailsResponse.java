package sandri.sandriweb.domain.dataImport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Google Places API (New) - Place Details 응답 DTO
 * https://places.googleapis.com/v1/places/{PLACE_ID}
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlaceDetailsResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("displayName")
    private DisplayName displayName;

    @JsonProperty("formattedAddress")
    private String formattedAddress;

    @JsonProperty("location")
    private Location location;

    @JsonProperty("photos")
    private List<Photo> photos;

    @JsonProperty("editorialSummary")
    private EditorialSummary editorialSummary;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DisplayName {
        @JsonProperty("text")
        private String text;

        @JsonProperty("languageCode")
        private String languageCode;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        @JsonProperty("latitude")
        private Double latitude;

        @JsonProperty("longitude")
        private Double longitude;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo {
        @JsonProperty("name")
        private String name;  // places/{PLACE_ID}/photos/{PHOTO_REFERENCE} 형식

        @JsonProperty("widthPx")
        private Integer widthPx;

        @JsonProperty("heightPx")
        private Integer heightPx;

        @JsonProperty("authorAttributions")
        private List<AuthorAttribution> authorAttributions;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AuthorAttribution {
        @JsonProperty("displayName")
        private String displayName;

        @JsonProperty("uri")
        private String uri;

        @JsonProperty("photoUri")
        private String photoUri;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EditorialSummary {
        @JsonProperty("text")
        private String text;

        @JsonProperty("languageCode")
        private String languageCode;
    }
}
