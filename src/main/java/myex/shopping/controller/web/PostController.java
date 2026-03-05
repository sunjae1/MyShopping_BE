package myex.shopping.controller.web;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.User;
import myex.shopping.dto.postdto.PostDBDto;
import myex.shopping.dto.postdto.PostEditDto;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.form.CommentForm;
import myex.shopping.form.PostForm;
import myex.shopping.repository.PostRepository;
import myex.shopping.service.PostService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Controller
@RequestMapping("/posts")
// @Validated //--> BindingResult 전에 예외 터트려서 전역으로 처리하게 함.(Valid + BindingResult
// 사용 못함.)
// 거의 클래스 레벨에서 사용해야 함. 메소드 위에 선언시 객체만 검증 가능.(ModelAttribute, RequestBody)
// 메서드 위는 단일 값 검증 RequestParam, PathVariable은 검증 못함. --> 단일 값 검증은 컨트롤러 안에서
// if문으로.
public class PostController {
    private final PostRepository postRepository;
    private final PostService postService;

    // 게시판 조회.
    @GetMapping
    public String list(Model model,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
            model.addAttribute("loginUser", userDto);
        }
        List<PostDBDto> posts = postService.findAllPostDBDto();
        // User loginUser = (User) session.getAttribute("loginUser");
        model.addAttribute("posts", posts);
        return "posts/list";
    }

    // 게시물 한개 상세 보기.
    @GetMapping("/{id}")
    public String view(@PathVariable Long id,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        log.info("post.id = {}", id);
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("loginUser", userDto);
            model.addAttribute("user", userDto);
        }
        /*
         * if (postOpt.isEmpty()) {
         * redirectAttributes.addFlashAttribute("errorPV","유효하지 않은 게시물입니다.");
         * return "redirect:/posts";
         * }
         */
        // LazyInitializationException 방지 위해서 DTO로 전환.
        // 뷰에서 LAZY 필드 접근 시 예외 막기 위해.
        PostDBDto postDBDto = postService.changeToDto(id);
        // model.addAttribute("loginUser",loginUser);
        model.addAttribute("post", postDBDto);
        model.addAttribute("commentForm", new CommentForm());
        return "posts/view";
    }

    // 게시물 등록 폼
    @GetMapping("/new")
    public String createForm(Model model,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        model.addAttribute("post", new PostForm());
        return "posts/new";
    }

    // 게시물 등록
    // <!--post(Form) : title, content, userId-->
    // Post(domain) : id(DB), title, content, userId, author, createdDate, comments
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("post") PostForm form,
            BindingResult bindingResult,
            Model model,
            HttpSession session) {
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        if (bindingResult.hasErrors()) {
            log.info("게시물 등록 폼 검증 실패 : {}", bindingResult);
            // model.addAttribute("user", loginUser);
            return "posts/new";
        }
        postService.createPost(form, loginUser);
        return "redirect:/posts";
    }

    // 게시물 수정 폼
    @GetMapping("/{id}/update")
    public String updateForm(@PathVariable Long id,
            Model model,
            HttpSession session) {

        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        PostDBDto postDBDto = postService.changeToDto(id);
        model.addAttribute("post", postDBDto);
        return "posts/edit";
    }

    // 게시물 수정
    @PostMapping("/{id}/update")
    public String updatePost(@PathVariable Long id,
            @Valid @ModelAttribute("post") PostEditDto form,
            BindingResult bindingResult,
            HttpSession session,
            Model model) {
        log.info("게시물 수정 요청 컨트롤러 진입");
        log.info("PostForm 정보 : {}", form);
        User loginUser = (User) session.getAttribute("loginUser");
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        if (bindingResult.hasErrors()) {
            log.info("검증 실패 : {}", bindingResult);
            return "posts/edit";
        }
        postService.updatePost(id, form);
        return "redirect:/posts/{id}";
        // @PostMapping("/{postId}/update") 경로 변수 {postId} 는 redirect:/{postId} 플레이스홀더
        // 값이랑 매칭 됨.
    }

    // 게시물 삭제.
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
            @RequestParam(required = false) String redirectInfo,
            RedirectAttributes redirectAttributes) {
        // 항상 null 가능성이 있는 변수를 .equals() 앞에 쓰면 NPE 위험
        log.info("redirectInfo 값 : {}", redirectInfo);
        postRepository.deleteById(id);
        if ("mypage".equals(redirectInfo)) {
            return "redirect:/mypage";
        }
        return "redirect:/posts";
    }

}
