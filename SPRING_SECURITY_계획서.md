# Spring Security 추가 계획서

## 📋 현재 상태 분석

### 현재 인증/인가 방식
- **인증 방식**: HttpSession 기반 수동 인증
- **인가 체크**: `LoginCheckInterceptor`를 통한 인터셉터 기반
- **비밀번호 저장**: 평문 저장 (보안 취약)
- **세션 관리**: 수동 세션 생성/관리

### 주요 문제점
1. ❌ 비밀번호 평문 저장 및 비교
2. ❌ 인터셉터 기반의 단순한 인증 체크
3. ❌ CSRF 보호 없음
4. ❌ 세션 하이재킹 방지 미흡
5. ❌ API와 Web 컨트롤러의 일관성 없는 인증 처리

---

## 🎯 Spring Security 추가 계획

### 1단계: 의존성 추가
```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
```

### 2단계: 핵심 구성 요소 생성

#### 2.1 SecurityConfig 클래스 생성
- URL 패턴별 접근 권한 설정
- 인증/인가 필터 체인 구성
- 세션 관리 설정
- CSRF 보호 설정

#### 2.2 UserDetailsService 구현
- 기존 `User` 엔티티를 Spring Security의 `UserDetails`로 변환
- 사용자 정보 로드 로직 구현

#### 2.3 PasswordEncoder 설정
- BCryptPasswordEncoder 사용
- 기존 평문 비밀번호를 해시로 변환하는 마이그레이션 필요

#### 2.4 Custom AuthenticationProvider (선택)
- 기존 `UserService.login()` 로직과 통합

### 3단계: 기존 코드 수정

#### 3.1 UserService 수정
- 비밀번호 암호화 저장
- 로그인 로직을 Spring Security 방식으로 변경

#### 3.2 Controller 수정
- `HttpSession`에서 `loginUser` 가져오는 부분을 `SecurityContext`에서 가져오도록 변경
- `@AuthenticationPrincipal` 어노테이션 활용

#### 3.3 Interceptor 제거
- `LoginCheckInterceptor` 제거
- `WebConfig`에서 인터셉터 등록 제거

#### 3.4 API 인증 처리
- API는 JWT 또는 세션 기반 인증 선택
- 현재는 세션 기반으로 통일

---

## 🔄 요청-응답 흐름 도식화

### 현재 구조 (Spring Security 적용 전)

```
[클라이언트] 
    │
    ├─ HTTP 요청
    │
    ▼
[DispatcherServlet]
    │
    ├─ LoginCheckInterceptor.preHandle()
    │   ├─ 세션 체크
    │   ├─ loginUser 없음? → /login 리다이렉트
    │   └─ loginUser 있음? → 통과
    │
    ▼
[Controller]
    │
    ├─ HttpSession에서 loginUser 추출
    │
    ▼
[Service Layer]
    │
    ▼
[Response 반환]
```

### Spring Security 적용 후 구조

```
[클라이언트]
    │
    ├─ HTTP 요청
    │
    ▼
[Spring Security Filter Chain]
    │
    ├─ [1] SecurityContextPersistenceFilter
    │   └─ SecurityContext 로드/저장
    │
    ├─ [2] UsernamePasswordAuthenticationFilter
    │   └─ /login POST 요청 처리
    │       ├─ UserDetailsService.loadUserByUsername()
    │       ├─ PasswordEncoder.matches()
    │       └─ Authentication 생성 → SecurityContext 저장
    │
    ├─ [3] LogoutFilter
    │   └─ /logout 요청 처리
    │
    ├─ [4] CsrfFilter
    │   └─ CSRF 토큰 검증
    │
    ├─ [5] FilterSecurityInterceptor
    │   └─ URL 패턴별 권한 체크
    │       ├─ 인증 필요? → Authentication 체크
    │       ├─ 권한 필요? → GrantedAuthority 체크
    │       └─ 실패 시 → AccessDeniedException 또는 AuthenticationException
    │
    ▼
[DispatcherServlet]
    │
    ▼
[Controller]
    │
    ├─ @AuthenticationPrincipal로 현재 사용자 조회
    │   또는 SecurityContextHolder.getContext().getAuthentication()
    │
    ▼
[Service Layer]
    │
    ▼
[Response 반환]
```

### 상세 흐름: 로그인 요청

```
1. [클라이언트] POST /login
   Body: { email: "user@example.com", password: "1234" }
   
2. [Spring Security] UsernamePasswordAuthenticationFilter
   ├─ 요청 가로채기
   ├─ email, password 추출
   └─ Authentication 객체 생성 (미인증 상태)
   
3. [AuthenticationManager] authenticate()
   ├─ UserDetailsService.loadUserByUsername(email)
   │   └─ DB에서 User 조회
   │
   ├─ PasswordEncoder.matches(rawPassword, encodedPassword)
   │   └─ 비밀번호 검증
   │
   └─ 인증 성공 시:
       ├─ Authentication 객체 생성 (인증 완료)
       ├─ UserDetails 정보 포함
       └─ GrantedAuthority 설정
   
4. [SecurityContext] 저장
   └─ SecurityContextHolder.getContext().setAuthentication(authenticated)
   
5. [Session] 저장
   └─ HttpSession에 SecurityContext 저장
   
6. [Controller] 호출 (선택적)
   └─ 로그인 성공 후 추가 처리 가능
   
7. [Response] 반환
   └─ 200 OK 또는 리다이렉트
```

### 상세 흐름: 인증된 요청

```
1. [클라이언트] GET /mypage
   Cookie: JSESSIONID=xxx
   
2. [SecurityContextPersistenceFilter]
   ├─ HttpSession에서 SecurityContext 로드
   └─ SecurityContextHolder에 설정
   
3. [FilterSecurityInterceptor]
   ├─ SecurityConfig 규칙 확인
   │   └─ /mypage는 인증 필요
   ├─ SecurityContext에서 Authentication 확인
   │   └─ 인증됨? → 통과
   │   └─ 미인증? → /login 리다이렉트
   └─ 권한 체크 (필요 시)
   
4. [DispatcherServlet]
   └─ 요청 라우팅
   
5. [Controller]
   ├─ @AuthenticationPrincipal UserDetails user
   │   또는
   └─ Authentication auth = SecurityContextHolder.getContext().getAuthentication()
   
6. [Service Layer]
   └─ 비즈니스 로직 처리
   
7. [Response] 반환
   └─ 200 OK + 데이터
```

---

## 📊 영향 분석

### 변경이 필요한 파일

#### 1. build.gradle
- ✅ Spring Security 의존성 추가

#### 2. 새로 생성할 파일
- ✅ `SecurityConfig.java` - 보안 설정
- ✅ `CustomUserDetailsService.java` - UserDetailsService 구현
- ✅ `CustomUserDetails.java` - UserDetails 구현체 (선택)

#### 3. 수정이 필요한 파일
- ⚠️ `UserService.java` - 비밀번호 암호화 로직 추가
- ⚠️ `UserController.java` - 세션 대신 SecurityContext 사용
- ⚠️ `ApiUserController.java` - 세션 대신 SecurityContext 사용
- ⚠️ `WebConfig.java` - LoginCheckInterceptor 제거
- ⚠️ 모든 Controller - HttpSession.getAttribute("loginUser") 제거

#### 4. 제거할 파일
- ❌ `LoginCheckInterceptor.java` - Spring Security로 대체

#### 5. 데이터베이스 마이그레이션
- ⚠️ 기존 평문 비밀번호를 해시로 변환하는 스크립트 필요

---

## ⚠️ 주의사항 및 고려사항

### 1. 세션 관리
- 현재 HttpSession 기반이므로 Spring Security도 세션 기반으로 설정
- 세션 타임아웃 설정 필요
- 동시 세션 제어 가능

### 2. CSRF 보호
- Thymeleaf 사용 시 자동 CSRF 토큰 처리
- API는 CSRF 토큰 또는 별도 설정 필요

### 3. 비밀번호 마이그레이션
- 기존 사용자 비밀번호를 BCrypt로 변환
- 회원가입 시 자동 암호화
- 로그인 시 기존 평문 비밀번호 체크 후 암호화 저장 (일회성)

### 4. API 인증
- 현재 `/api/**`는 인터셉터에서 제외됨
- Spring Security 적용 시 API도 인증 필요 여부 결정 필요

### 5. 테스트 코드
- Spring Security 적용 시 테스트 코드 수정 필요
- `@WithMockUser` 또는 `SecurityContext` 설정 필요

---

## 🚀 구현 단계별 체크리스트

### Phase 1: 기본 설정
- [ ] Spring Security 의존성 추가
- [ ] SecurityConfig 기본 구조 생성
- [ ] 모든 요청 허용 설정 (임시)

### Phase 2: 인증 구현
- [ ] UserDetailsService 구현
- [ ] PasswordEncoder 설정
- [ ] 로그인 폼 설정
- [ ] 로그인 처리 필터 설정

### Phase 3: 인가 설정
- [ ] URL 패턴별 권한 설정
- [ ] 공개 경로 설정 (/, /login, /register 등)
- [ ] 인증 필요 경로 설정

### Phase 4: 기존 코드 통합
- [ ] UserService 비밀번호 암호화
- [ ] Controller에서 SecurityContext 사용
- [ ] Interceptor 제거

### Phase 5: 테스트 및 검증
- [ ] 로그인/로그아웃 테스트
- [ ] 권한 체크 테스트
- [ ] 기존 기능 회귀 테스트

---

## 📝 예상 코드 변경 예시

### SecurityConfig.java (새 파일)
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/api/**").permitAll()
                .requestMatchers("/mypage", "/cart/**", "/order/**").authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
            )
            .sessionManagement(session -> session
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### Controller 변경 예시
```java
// 변경 전
@GetMapping("/mypage")
public String myPage(HttpSession session, Model model) {
    User loginUser = (User) session.getAttribute("loginUser");
    // ...
}

// 변경 후
@GetMapping("/mypage")
public String myPage(@AuthenticationPrincipal CustomUserDetails userDetails, Model model) {
    User loginUser = userDetails.getUser();
    // ...
}
```

---

## ✅ 승인 요청

위 계획서를 검토하시고, Spring Security 추가 작업을 진행해도 되는지 확인 부탁드립니다.

**주요 변경 사항 요약:**
1. Spring Security 의존성 추가
2. SecurityConfig 및 UserDetailsService 구현
3. 비밀번호 암호화 적용
4. 기존 Interceptor 제거 및 Controller 수정
5. 세션 기반 인증 유지

진행 여부를 알려주시면 단계별로 구현하겠습니다.

