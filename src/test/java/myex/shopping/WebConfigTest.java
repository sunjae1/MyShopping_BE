package myex.shopping;

import myex.shopping.interceptor.LoginCheckInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WebConfigTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("/img 리소스 핸들러가 file.dir 경로를 사용한다")
    void addResourceHandlersUsesConfiguredDirectory() {
        Path uploadDir = tempDir.resolve("UploadFolder");
        String configuredUploadDir = uploadDir.toString().replace('\\', '/');

        WebConfig webConfig = new WebConfig(mock(LoginCheckInterceptor.class));
        ReflectionTestUtils.setField(webConfig, "uploadDir", configuredUploadDir);

        MockServletContext servletContext = new MockServletContext();
        GenericWebApplicationContext applicationContext = new GenericWebApplicationContext(servletContext);
        applicationContext.refresh();

        ResourceHandlerRegistry registry = new ResourceHandlerRegistry(applicationContext, servletContext);
        webConfig.addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/img/**")).isTrue();

        @SuppressWarnings("unchecked")
        List<Object> registrations = (List<Object>) ReflectionTestUtils.getField(registry, "registrations");
        assertThat(registrations).isNotEmpty();

        Object imageRegistration = registrations.getFirst();
        @SuppressWarnings("unchecked")
        List<String> locationValues = (List<String>) ReflectionTestUtils.getField(imageRegistration, "locationValues");

        assertThat(locationValues).contains("file:" + configuredUploadDir + "/");
    }
}
