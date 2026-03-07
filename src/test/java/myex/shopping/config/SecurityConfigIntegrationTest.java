package myex.shopping.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("비로그인 사용자도 /img 경로에 접근할 수 있다")
    void imgPathShouldBeAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/img/not-found-image.png"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"));
    }
}

