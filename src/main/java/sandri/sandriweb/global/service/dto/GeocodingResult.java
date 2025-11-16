package sandri.sandriweb.global.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeocodingResult {
    private final double latitude;
    private final double longitude;
    private final String formattedAddress;
}

