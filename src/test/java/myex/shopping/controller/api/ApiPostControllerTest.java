package myex.shopping.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.dto.postdto.PostEditDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.form.PostForm;
import myex.shopping.repository.PostRepository;
import myex.shopping.repository.UserRepository;
import myex.shopping.service.PostService;
import myex.shopping.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiPostControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostService postService;
    @Autowired
    private UserService userService;
    @Autowired
    private EntityManager em;

    private User testUser;
    private User otherUser;
    private Post testPost;
    private PrincipalDetails testUserDetails;
    private PrincipalDetails otherUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser@example.com", "테스트유저", "password");
        otherUser = new User("otheruser@example.com", "다른유저", "password");
        userService.save(testUser);
        userService.save(otherUser);

        testUserDetails = new PrincipalDetails(testUser);
        otherUserDetails = new PrincipalDetails(otherUser);

        Post post = new Post("테스트 제목", "테스트 내용");
        testPost = postService.addUser(post, testUser.getId());
        postRepository.save(testPost);

        em.flush();
        em.clear();
    }

    @Test
    @WithMockUser
    @DisplayName("전체 게시물 조회 API 테스트: GET /api/posts")
    void getAllPosts_shouldReturnPostList() throws Exception {
        mockMvc.perform(get("/api/posts"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("테스트 제목")))
                .andExpect(jsonPath("$[0].authorName", is(testUser.getName())));
    }

    @Test
    @WithMockUser
    @DisplayName("단일 게시물 조회 API 테스트: GET /api/posts/{id}")
    void getPost_shouldReturnPost() throws Exception {
        mockMvc.perform(get("/api/posts/{id}", testPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title", is(testPost.getTitle())))
                .andExpect(jsonPath("$.content", is(testPost.getContent())))
                .andExpect(jsonPath("$.authorName", is(testUser.getName())));
    }

    @Test
    @DisplayName("게시물 생성 API 테스트: POST /api/posts")
    void createPost_shouldCreatePost() throws Exception {
        PostForm postForm = new PostForm();
        postForm.setTitle("새로운 게시물");
        postForm.setContent("새로운 내용입니다.");

        mockMvc.perform(post("/api/posts")
                .with(user(testUserDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(postForm)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("새로운 게시물")))
                .andExpect(jsonPath("$.authorName", is(testUser.getName())));
    }

    @Test
    @DisplayName("게시물 수정 API 성공 테스트 (작성자): PUT /api/posts/{id}")
    void updatePost_shouldUpdatePost_whenUserIsOwner() throws Exception {
        PostEditDto editDto = new PostEditDto();
        editDto.setTitle("수정된 제목");
        editDto.setContent("수정된 내용입니다.");

        mockMvc.perform(put("/api/posts/{id}", testPost.getId())
                .with(user(testUserDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(editDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("수정된 제목")))
                .andExpect(jsonPath("$.content", is("수정된 내용입니다.")));
    }

    @Test
    @DisplayName("게시물 삭제 API 성공 테스트 (작성자): DELETE /api/posts/{id}")
    void deletePost_shouldDeletePost_whenUserIsOwner() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}", testPost.getId())
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isNoContent());

        assertThat(postRepository.findById(testPost.getId())).isEmpty();
    }

    @Test
    @DisplayName("게시물 삭제 API 실패 테스트 (작성자 아님): DELETE /api/posts/{id}")
    void deletePost_shouldFail_whenUserIsNotOwner() throws Exception {
        mockMvc.perform(delete("/api/posts/{id}", testPost.getId())
                .with(user(otherUserDetails)))
                .andDo(print())
                .andExpect(status().isForbidden());

        assertThat(postRepository.findById(testPost.getId())).isNotEmpty();
    }
}
