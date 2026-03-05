package myex.shopping.service;

import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Cart;
import myex.shopping.domain.CartItem;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.dto.cartdto.CartDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {
    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final EntityManager em;

    @Transactional(readOnly = false)
    public Cart findOrCreateCartForUser(User sessionUser) {
        // 로그인 처리 예외로 바꾸기.
        if (sessionUser == null) {
            throw new ResourceNotFoundException("user not found");
        }
        User user = userRepository.findById(sessionUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));

        // 사용자별 장바구니 찾거나 새로 만들어서 사용자와 연결 후 반환.
        return cartRepository.findByUser(sessionUser)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    // 더티체킹.
                    user.addCart(newCart);
                    log.info("findOrCreateCartForUser 메소드 끝"); // 더티 체킹 : 메소드 끝나는 commit 시점에 INSERT 문 실행.
                    return newCart; // 지역변수로 newCart는 사라지지만 객체 주소를 가진 외부 변수가 있으므로 new Cart();는 GC 안됨.
                });
    }

    // 장바구니 전체 DTO 변환
    @Transactional(readOnly = true)
    public CartDto findByUserByDto(User user) {
        return cartRepository.findByUser(user)
                .map(CartDto::new)
                .orElse(null); // null 해야 프론트에서 가능.

    }

    @Transactional(readOnly = false)
    public void save(Cart cart, User loginUser) {
        cartRepository.save(cart);
        log.info("cart 저장 후 cart 정보: {}", cart);
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        user.addCart(cart);
        log.info("cartService.save: user.Carts = {}", user.getCarts());
    }

    @Transactional(readOnly = false)
    public void update(Cart cart) {
        log.info("준영속 객체 Cart : {}", cart);

        Cart managedCart = em.merge(cart); // update문 예약.
        log.info("em.flush(); 전");
        em.flush(); // id는 hibernate가 merge()호출 중에 IdentifierGenerator 로 바로 ID 생성.
        // (다음 줄에 바로 Native Query가 나가지 않는 이상 지금 상태에선 필요 없음)
        log.info("em.flush() 후");

        // 세션과 영속 객체 같게 맞춤. --> cart: DB에서 꺼낸 준영속, managedCart : 영속 상태\
        log.info("DB에서 꺼낸 준영속 Cart.getId : {}", cart.getId());
        log.info("em.merge 후 영속 상태 manaedCart.getId : {}", managedCart.getId());
        // cart.setId(managedCart.getId());
        // cart.getCartItems().clear();

        /*
         * //DB-> 세션과 동기화.
         * for (CartItem managedCI : managedCart.getCartItems()) {
         * CartItem sessionCI = new CartItem();
         * sessionCI.setId(managedCI.getId());
         * sessionCI.setItem(managedCI.getItem());
         * sessionCI.setQuantity(managedCI.getQuantity());
         * 
         * //양방향 연관관계
         * sessionCI.setCart(cart);
         * cart.getCartItems().add(sessionCI);
         * }
         */
    }

    @Transactional(readOnly = false)
    public Cart deleteItem(Long itemId, User loginUser) {
        // 없는 아이템 삭제 -> 예외 발생.
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("cart not found"));
        Cart cart = findOrCreateCartForUser(loginUser);
        // 더티 체킹 - Commit 시점에 delete 쿼리 실행됨.
        cart.removeItem(item);
        return cart;
    }

    @Transactional(readOnly = false)
    public void deleteCart(Long cartId, Long userId) {
        log.info("cart 조회 전");
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("cart not found"));
        log.info("cart 조회 후");
        log.info("user 조회 전");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));
        log.info("user 조회 후");
        // User CASCADETYPE.ALL 이라서 Cart에도 전파(더티 체킹)
        user.deleteCart(cart);
        log.info("deleteCart 후");
    }
}
