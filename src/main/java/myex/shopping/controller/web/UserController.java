package myex.shopping.controller.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.*;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.dto.mypagedto.MyPageOrderDto;
import myex.shopping.dto.mypagedto.MyPagePostDBDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.form.LoginForm;
import myex.shopping.form.RegisterForm;
import myex.shopping.repository.ItemRepository;
import myex.shopping.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService; //생성자
    private final OrderService orderService;
    private final PostService postService;
    private final CartService cartService;
    private final ItemService itemService;

    //로그인 페이지로 보내기.
    @GetMapping("/login")
    public String start(Model model,
                        HttpSession session) {
        model.addAttribute("form", new LoginForm());
        //인터셉터 -> 로그인 페이지 리다이렉트(로그인 필요 메시지 출력)

        //Spring Security가 저장한 원래 요청이 있는지 확인.
        if (session.getAttribute("SPRING_SECURITY_SAVED_REQUEST") != null) {
            model.addAttribute("LoginMessage", "로그인이 필요한 작업입니다.");
        }
        return "login";
    }

    //회원가입 폼 조회.
    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "register";
    }
    //회원가입 등록.
    @PostMapping("/register")
    public String addUser(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            log.info("회원 가입 검증 실패 - {}", bindingResult);
            return "register";
        }
        User user = new User(form.getEmail(), form.getName(), form.getPassword());
        //회원가입 성공 시
        userService.save(user);
        return "redirect:/login";
    }
    //전체 회원 목록 조회.
    @GetMapping("/allUser")
    public String allUser(Model model) {
        List<User> users = userService.allUser();
        model.addAttribute("users", users);
        return "allUser";
    }
    //메인 페이지 요청 : Item, User (+검색 추가)
    @GetMapping("/")
    public String mainPage(Model model,
                           @AuthenticationPrincipal(expression = "#this == 'anonymousUser' ? null : principalDetails")
                           PrincipalDetails principalDetails,
                           @RequestParam(required = false) String keyword,
                           @RequestParam(required = false) Long categoryId) {
        if (principalDetails != null) { //인증된 사용자일 경우에만 처리
            User loginUser = principalDetails.getUser();
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        List<ItemDto> items = itemService.findItems(keyword, categoryId);
        model.addAttribute("items", items);
        return "main";
    }
    //마이페이지 보내는거 : user, orders, posts, cart
    @GetMapping("/mypage")
    public String myPage(@AuthenticationPrincipal PrincipalDetails principalDetails,
                         Model model)
    {
        User loginUser = principalDetails.getUser();
        UserDto userDto = new UserDto(loginUser);
        model.addAttribute("user",userDto);

        Cart cart = cartService.findOrCreateCartForUser(loginUser);
        log.info("cart.getId() : {}", cart.getId());
        List<MyPageOrderDto> orderDtos = orderService.changeToOrderDtoList(loginUser);
        List<MyPagePostDBDto> postDtos = postService.changeToDtoList(loginUser);

        log.info("orders DTO 정보 : {}",orderDtos);

        model.addAttribute("orders", orderDtos);
        model.addAttribute("posts", postDtos);
        model.addAttribute("cart",cart);

        return "mypage/view";
    }

    private static Cart getOrCreateCart(HttpSession session) {
        Cart cart = (Cart) session.getAttribute("CART");
        if (cart == null) {
            cart = new Cart();
            session.setAttribute("CART", cart);
        }
        return cart;
    }

}
