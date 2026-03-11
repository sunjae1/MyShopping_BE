package myex.shopping.dto.itemdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import myex.shopping.domain.Item;
import org.springframework.format.annotation.NumberFormat;

@Getter
@Schema(description = "한 상품의 상세 정보 담는 DTO")
public class ItemDtoDetail {
    @Schema(description = "상품ID", example = "1")
    private Long id;
    @Schema(description = "상품이름", example = "아이템A")
    private String name;
    @Schema(description = "상품가격", example = "2000")
    @NumberFormat(pattern = "#,###")
    private int price;
    @Schema(description = "상품재고 수량", example = "30")
    private int quantity;
    @Schema(description = "상품 이미지 URL (Pre-signed URL)", example = "https://bucket.s3.amazonaws.com/images/1.webp?X-Amz-...")
    @Setter
    private String imageUrl;

    public ItemDtoDetail(Item item) {
        this.id = item.getId();
        this.name = item.getItemName();
        this.price = item.getPrice();
        this.quantity = item.getQuantity();
        this.imageUrl = item.getImageUrl();
    }
}
