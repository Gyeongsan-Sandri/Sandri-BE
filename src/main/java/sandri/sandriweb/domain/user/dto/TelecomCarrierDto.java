package sandri.sandriweb.domain.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TelecomCarrierDto {
    
    private String code;
    private String name;
    
    public static TelecomCarrierDto[] getCarriers() {
        return new TelecomCarrierDto[]{
                TelecomCarrierDto.builder().code("KT").name("KT").build(),
                TelecomCarrierDto.builder().code("SKT").name("SKT").build(),
                TelecomCarrierDto.builder().code("LG_U_PLUS").name("LG U+").build(),
                TelecomCarrierDto.builder().code("KT_MVNO").name("KT 알뜰폰").build(),
                TelecomCarrierDto.builder().code("SKT_MVNO").name("SKT 알뜰폰").build(),
                TelecomCarrierDto.builder().code("LG_U_PLUS_MVNO").name("LG U+ 알뜰폰").build()
        };
    }
}
