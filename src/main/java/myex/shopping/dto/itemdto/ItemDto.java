package myex.shopping.dto.itemdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import myex.shopping.domain.Item;
import org.springframework.format.annotation.NumberFormat;

@Getter
@Schema(description = "상품 정보를 담는 DTO")
public class ItemDto {
    @Schema(description = "상품ID", example = "1")
    private Long id;
    @Schema(description = "상품이름", example = "아이템A")
    private String itemName;
    @Schema(description = "상품가격", example = "2000")
    @NumberFormat(pattern = "#,###") //1,000 문자열 입력시 자동 파싱 수행.
    private int price;
    @Schema(description = "상품재고 수량", example = "30")
    private int quantity;
    @Schema(description = "상품 이미지 URL (Pre-signed URL)", example = "https://bucket.s3.amazonaws.com/images/1.webp?X-Amz-...")
    @Setter
    private String imageUrl;

    public ItemDto(Item item) {
        this.id = item.getId();
        this.itemName = item.getItemName();
        this.price = item.getPrice();
        this.quantity = item.getQuantity();
        this.imageUrl = item.getImageUrl();
    }
}
