package myex.shopping.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.User;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.dto.userdto.UserEditDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.List;
import java.util.Optional;


//클래스 레벨에 쓰며 자동 @Bean 등록 및 계층 구분 의미
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 빈 스캔해서 타입에 맞는 빈을 찾아 자동으로 DI 연결.
    // 타입 여러 개면 @Primary, @Qualifier 사용해서 우선순위 설정.
    // 하나밖에 없으면 자동 주입됨.
    // DI를 수행하도록 표시 : 생성자, 수정자, 필드 주입. (생성자 하나라면 @Autowired 생략 가능)

    //회원가입 사용자 저장
    //email, name, password 입력.
    @Transactional(readOnly = false)
    public Long save(User user) {
        user.setActive(true);
        //비밀번호 암호화
        user.setPassword(passwordEncoder.encode(user.getPassword())); //Encode Password
        User savedUser = userRepository.save(user);
        log.info("{}가 저장되었습니다.",savedUser);
        return savedUser.getId();
    }
    //로그인 로직.
    //Spring Security가 대신 수행.

    //Spring Security가 인증 과정에서 호출.(사용자 로그인 시도 시)
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        return new PrincipalDetails(user);
    }

    //전체 사용자 조회
    public List<User> allUser() {
        return userRepository.findAll();
    }

    //활성화된 전체 사용자 조회
    public List<User> allUserByActive() {
        return userRepository.findAllByActiveTrue();
    }

    //회원 정보 수정 (name, email)
    //더티 체킹
    @Transactional(readOnly = false)
    public User updateUser(Long id, UserEditDto updateDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));
        if (updateDTO.getName() != null) {
            user.setName(updateDTO.getName());
        }
        if (updateDTO.getEmail() != null) {
            user.setEmail(updateDTO.getEmail());
        }
        return user;
    }

    //사용자 삭제. -> active로 구분.(soft-delete)
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
