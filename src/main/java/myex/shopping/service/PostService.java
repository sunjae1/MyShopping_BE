package myex.shopping.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Post;
import myex.shopping.domain.User;
import myex.shopping.dto.mypagedto.MyPagePostDBDto;
import myex.shopping.dto.postdto.PostDBDto;
import myex.shopping.dto.postdto.PostDto;
import myex.shopping.dto.postdto.PostEditDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.form.PostForm;
import myex.shopping.repository.PostRepository;
import myex.shopping.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    // 게시물 저장.
    public Post createPost(PostForm form, User loginUser) {
        Post post = new Post();
        post.setTitle(form.getTitle());
        post.setContent(form.getContent());
        post.setCreatedDate(LocalDateTime.now());
        post.setAuthor(loginUser.getName());
        addUser(post, loginUser.getId());
        log.info("post 저장 후");
        return post;
    }

    public Post addUser(Post post, Long userId) {
        User persistUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        post.addUser(persistUser);
        log.info("addUser 연관관계 설정 후");
        // post em.persist()
        return post;
    }

    public PostDBDto changeToDto(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found"));
        return new PostDBDto(post); // 트랜잭션 내 user, comments 접근
    }

    public List<MyPagePostDBDto> changeToDtoList(User user) {
        return postRepository.findByUser(user)
                .stream()
                .map(MyPagePostDBDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostDto> findPostDtosByUser(User user) {
        return postRepository.findByUser(user)
                .stream()
                .map(PostDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostDBDto> findAllPostDBDto() {
        // 1. fetch join으로 최적화 된 finAll()호출(쿼리 1번, Post - User, Comments)
        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .map(PostDBDto::new) // post -> new PostDBDto(post) 동일.
                .collect(Collectors.toList());
    }

    public PostDto updatePost(Long id, @Valid PostEditDto form) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post Not Found"));
        post.setTitle(form.getTitle());
        post.setContent(form.getContent());
        // @Transactional 안에서 DTO로 변환시 LAZY 연관 예외 터지지 않음.
        return new PostDto(post);
    }

    // 게시물 작성자 본인인지 확인 메소드
    public boolean isPostOwner(Long id, User loginUser) {
        if (loginUser == null) {
            return false;
        }
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return post.getUser().getId().equals(loginUser.getId());
    }
}
