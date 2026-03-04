package myex.shopping;

import com.fasterxml.jackson.databind.ObjectMapper;
import myex.shopping.domain.*;
import myex.shopping.dto.userdto.LoginRequestDto;
import myex.shopping.form.CartForm;
import myex.shopping.form.PostForm;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.OrderRepository;
import myex.shopping.repository.UserRepository;
import myex.shopping.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ImageService imageService;

    @BeforeEach
    void setupUsersForApiLoginTests() {
        // Given: Create users for API login tests in a separate setup method
        // to ensure they are committed before the test execution.
        if (userRepository.findByEmail("apiuser@example.com").isEmpty()) {
            User user1 = new User("apiuser@example.com", "Api User", passwordEncoder.encode("password"));
            user1.setActive(true); // 사용자를 활성 상태로 설정
            userRepository.save(user1);
        }
        if (userRepository.findByEmail("apiuser2@example.com").isEmpty()) {
            User user2 = new User("apiuser2@example.com", "Api User 2", passwordEncoder.encode("password"));
            user2.setActive(true); // 사용자를 활성 상태로 설정
            userRepository.save(user2);
        }
    }




    @Test
    @DisplayName("사용자 전체 시나리오 통합 테스트: 회원가입 -> 로그인 -> 상품 추가 -> 장바구니 담기 -> 주문 -> 게시글 작성")
    void fullUserJourneyTest() throws Exception {
        when(imageService.storeFile(any())).thenReturn("/fake/path/image.jpg");

        // 1. 회원가입
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Test User")
                        .param("email", "testuser@example.com")
                        .param("password", "password")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // 2. 로그인
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "testuser@example.com")
                        .param("password", "password")
                        .session(session) // Pass session to login
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(request().sessionAttribute("loginUser", is(notNullValue())))
                .andExpect(request().sessionAttribute("loginUser", isA(User.class)));


        // 3. 상품 추가 (웹 UI 통해)
        MvcResult itemAddResult = mockMvc.perform(post("/items/add")
                        .session(session) // Use the same session
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("itemName", "Test Item")
                        .param("price", "10000")
                        .param("quantity", "100")
                )
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectedUrl = itemAddResult.getResponse().getRedirectedUrl();
        long itemId = Long.parseLong(redirectedUrl.substring(redirectedUrl.lastIndexOf('/') + 1));

        // 3.1 추가된 상품 조회하여 확인
        mockMvc.perform(get("/items/{itemId}", itemId).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test Item")));

        // 4. 장바구니에 상품 담기
        // [수정] RequestBody로 보낼 객체 생성
        CartForm cartForm = new CartForm();
        cartForm.setId(itemId);
        cartForm.setQuantity(1); // 수량 설정 (재고 100개보다 적게)

        mockMvc.perform(post("/api/items/{itemId}/cart", itemId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON) // [수정] JSON 타입 명시
                        .content(objectMapper.writeValueAsString(cartForm)) // [수정] Body 데이터 추가
                )
                .andDo(print()) // [팁] 디버깅을 위해 응답 로그 출력
                .andExpect(status().isOk());

        // 5. 주문하기
        mockMvc.perform(post("/items/order")
                        .session(session)
                )
                .andExpect(status().is3xxRedirection());
        
        // 5.1 마이페이지에서 주문 내역 확인
         mockMvc.perform(get("/mypage").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test Item")));

        // 6. 게시글 작성
        PostForm postForm = new PostForm();
        postForm.setTitle("New Post Title");
        postForm.setContent("This is the content of the new post.");

        mockMvc.perform(post("/api/posts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postForm)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Post Title"));
    }

    //보안 취약점 -> 수정 요구사항.
    @Test
    @DisplayName("보안 테스트: 인증 없이 상품 추가 가능 (multipart/form-data)")
    void unauthorizedItemCreationTest() throws Exception {
        when(imageService.storeFile(any())).thenReturn("/fake/path/unauthorized.jpg");

        // API 엔드포인트는 multipart/form-data를 소비하므로 .param()을 사용
        MvcResult result = mockMvc.perform(multipart("/api/items/add")
                        .param("itemName", "Unauthorized Item")
                        .param("price", "5000")
                        .param("quantity", "50")
                )
                .andExpect(status().isCreated())
                .andReturn();

        String createdUrl = result.getResponse().getHeader("Location");
        
        // 2. 생성된 아이템을 GET으로 조회하여 확인
        mockMvc.perform(get(createdUrl))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName").value("Unauthorized Item"));
    }

    @Test
    @DisplayName("주문 취소 실패: 다른 사용자의 주문을 취소할 수 없다")
    void cancelOrder_Fails_WhenUserIsNotOwner() throws Exception {
        // given
        // User A와 주문 생성
        User userA = new User("userA@example.com", "User A", "password");
        userRepository.save(userA);
        Item item = new Item("Some Item", 100, 10, "path");
        itemRepository.save(item);
        Order order = new Order(userA);
        order.addOrderItem(new OrderItem(item, item.getPrice(), 1));
        orderRepository.save(order);

        // User B 생성 및 로그인
        User userB = new User("userB@example.com", "User B", "password");
        userRepository.save(userB);
        MockHttpSession userBSession = new MockHttpSession(); //가짜 빈 세션
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "userB@example.com")
                .param("password", "password")
                .session(userBSession)); //가짜 세션 보내서 로그인 성공 시, 이 세션 안에 '로그인 한 userB의 정보' 저장해 돌려줌.

        // when & then
        // User B가 User A의 주문 취소 시도
        mockMvc.perform(post("/items/{id}/cancel", order.getId())
                        .session(userBSession))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("주문 취소 성공: 사용자가 자신의 주문을 취소한다")
    void cancelOrder_Succeeds_WhenUserIsOwner() throws Exception {
        // given
        // User A와 주문 생성
        User userA = new User("userA@example.com", "User A", "password");
        userRepository.save(userA);
        Item item = new Item("Some Item", 100, 10, "path");
        itemRepository.save(item);
        Order order = new Order(userA);
        order.addOrderItem(new OrderItem(item, item.getPrice(), 1));
        orderRepository.save(order);

        // User A로 로그인
        MockHttpSession userASession = new MockHttpSession();
        mockMvc.perform(post("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("email", "userA@example.com")
                .param("password", "password")
                .session(userASession));

        // when & then
        // User A가 자신의 주문 취소 시도
        mockMvc.perform(post("/items/{id}/cancel", order.getId())
                        .session(userASession))
                .andExpect(status().is3xxRedirection());

        Order cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
    @Test
    @DisplayName("API 로그인 성공 테스트")
    void apiLogin_Success() throws Exception {
        // given
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("apiuser@example.com");
        loginRequest.setPassword("password");

        // when & then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.email").value("apiuser@example.com"))
                .andExpect(jsonPath("$.name").value("Api User"));
    }

    @Test
    @DisplayName("API 로그인 실패 테스트: 잘못된 비밀번호")
    void apiLogin_Fails_WithWrongPassword() throws Exception {
        // given
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("apiuser2@example.com");
        loginRequest.setPassword("wrongpassword");

        // when & then
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }


}
