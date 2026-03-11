package myex.shopping.controller.api;

import myex.shopping.domain.Item;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.dto.itemdto.ItemDtoDetail;
import myex.shopping.dto.itemdto.ItemEditDto;
import myex.shopping.repository.ItemRepository;
import myex.shopping.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ApiItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @MockitoBean
    private ImageService imageService;

    private Item testItem1;
    private Item testItem2;

    @BeforeEach
    void setUp() throws IOException {
        // ImageService.storeFile이 항상 가짜 S3 key를 반환하도록 설정
        when(imageService.storeFile(any())).thenReturn("images/fake-image.jpg");

        // resolveImageUrls/resolveImageUrl이 입력을 그대로 반환하도록 설정
        when(imageService.resolveImageUrls(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(ItemDto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(ItemDtoDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(ItemEditDto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.generatePresignedUrl(any())).thenReturn("https://fake-presigned-url.com/image.jpg");

        testItem1 = new Item("테스트 상품 1", 10000, 10, "images/test1.jpg");
        testItem2 = new Item("테스트 상품 2", 25000, 5, "images/test2.jpg");
        itemRepository.save(testItem1);
        itemRepository.save(testItem2);
    }

    @Test
    @DisplayName("전체 상품 조회 API 테스트: GET /api/items")
    void getItems_shouldReturnItemList() throws Exception {
        mockMvc.perform(get("/api/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].itemName", is(testItem1.getItemName())))
                .andExpect(jsonPath("$[0].price", is(testItem1.getPrice())))
                .andExpect(jsonPath("$[1].itemName", is(testItem2.getItemName())));
    }

    @Test
    @DisplayName("개별 상품 조회 API 테스트: GET /api/items/{itemId}")
    void getItem_shouldReturnItem() throws Exception {
        Long itemId = testItem1.getId();

        mockMvc.perform(get("/api/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.itemName", is(testItem1.getItemName())))
                .andExpect(jsonPath("$.price", is(testItem1.getPrice())))
                .andExpect(jsonPath("$.quantity", is(testItem1.getQuantity())));
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 404 Not Found 반환")
    void getItem_shouldReturnNotFound_whenItemDoesNotExist() throws Exception {
        Long nonExistentItemId = 99999L;

        mockMvc.perform(get("/api/items/{itemId}", nonExistentItemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 추가 등록 API 테스트: POST /api/items")
    void addItem_shouldCreateItem() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "test-image.jpg", "image/jpeg",
                "image_content".getBytes());

        mockMvc.perform(multipart("/api/items")
                        .file(imageFile)
                        .param("itemName", "새로운 상품")
                        .param("price", "30000")
                        .param("quantity", "20")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemName", is("새로운 상품")))
                .andExpect(jsonPath("$.price", is(30000)))
                .andExpect(header().exists("Location"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 수정 API 테스트: PUT /api/items/{id}")
    void editItem_shouldUpdateItem() throws Exception {
        Long itemId = testItem1.getId();
        MockMultipartFile newImageFile = new MockMultipartFile("imageFile", "new-image.jpg", "image/jpeg",
                "new_image_content".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/items/{itemId}", itemId)
                        .file(newImageFile)
                        .param("itemName", "수정된 상품 이름")
                        .param("price", "12000")
                        .param("quantity", "5")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName", is("수정된 상품 이름")))
                .andExpect(jsonPath("$.price", is(12000)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 삭제 API 테스트: DELETE /api/items/{id}")
    void deleteItem_shouldRemoveItem() throws Exception {
        Long itemId = testItem1.getId();

        mockMvc.perform(delete("/api/items/{itemId}", itemId))
                .andDo(print())
                .andExpect(status().isNoContent());

        assertThat(itemRepository.findById(itemId)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 이미지 없이 등록하면 400을 반환한다")
    void addItem_shouldReturnBadRequest_whenImageFileIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/items")
                        .param("itemName", "이미지 없는 상품")
                        .param("price", "30000")
                        .param("quantity", "20")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.imageFile", is("상품 이미지를 선택해주세요.")));
    }
}