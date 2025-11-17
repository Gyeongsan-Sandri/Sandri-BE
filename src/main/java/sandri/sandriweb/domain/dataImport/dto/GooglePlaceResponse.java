package sandri.sandriweb.domain.dataImport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GooglePlaceResponse {

    @JsonProperty("candidates")
    private List<Candidate> candidates;

    @JsonProperty("status")
    private String status;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Candidate {
        @JsonProperty("formatted_address")
        private String formattedAddress;

        @JsonProperty("geometry")
        private Geometry geometry;

        @JsonProperty("name")
        private String name;

        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("photos")
        private List<Photo> photos;

        @JsonProperty("editorial_summary")
        private EditorialSummary editorialSummary;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Geometry {
        @JsonProperty("location")
        private Location location;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Location {
        @JsonProperty("lat")
        private Double lat;

        @JsonProperty("lng")
        private Double lng;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Photo {
        @JsonProperty("photo_reference")
        private String photoReference;

        @JsonProperty("height")
        private Integer height;

        @JsonProperty("width")
        private Integer width;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EditorialSummary {
        @JsonProperty("overview")
        private String overview;
    }
}
