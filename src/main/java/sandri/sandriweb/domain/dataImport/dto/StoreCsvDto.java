package sandri.sandriweb.domain.dataImport.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class StoreCsvDto {

    @CsvBindByName(column = "상호명")
    private String storeName;

    @CsvBindByName(column = "지점명")
    private String branchName;

    @CsvBindByName(column = "상권업종소분류코드")
    private String industryCode;

    @CsvBindByName(column = "상권업종소분류명")
    private String industryName;

    @CsvBindByName(column = "시도명")
    private String province;

    @CsvBindByName(column = "시군구명")
    private String city;

    @CsvBindByName(column = "지번주소")
    private String jibunAddress;

    @CsvBindByName(column = "도로명주소")
    private String roadAddress;

    @CsvBindByName(column = "경도")
    private String longitude;

    @CsvBindByName(column = "위도")
    private String latitude;
}
