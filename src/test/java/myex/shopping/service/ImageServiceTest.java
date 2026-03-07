package myex.shopping.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ImageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("업로드 폴더가 없어도 생성하고 파일을 저장한다")
    void storeFileCreatesDirectoryAndStoresFile() throws IOException {
        ImageService imageService = new ImageService();
        Path uploadDir = tempDir.resolve("UploadFolder");
        ReflectionTestUtils.setField(imageService, "uploadDir", uploadDir.toString());

        MockMultipartFile multipartFile = new MockMultipartFile(
                "imageFile",
                "test-image.png",
                "image/png",
                "image-content".getBytes()
        );

        String imageUrl = imageService.storeFile(multipartFile);

        assertThat(imageUrl).startsWith("/img/").endsWith(".png");
        assertThat(Files.exists(uploadDir)).isTrue();
        assertThat(Files.list(uploadDir)).singleElement().satisfies(path -> {
            assertThat(path.getFileName().toString()).endsWith(".png");
            assertThat(path).hasBinaryContent("image-content".getBytes());
        });
    }

    @Test
    @DisplayName("빈 파일이면 저장하지 않고 null을 반환한다")
    void storeFileReturnsNullForEmptyFile() throws IOException {
        ImageService imageService = new ImageService();
        Path uploadDir = tempDir.resolve("UploadFolder");
        ReflectionTestUtils.setField(imageService, "uploadDir", uploadDir.toString());

        MockMultipartFile emptyFile = new MockMultipartFile(
                "imageFile",
                "empty.png",
                "image/png",
                new byte[0]
        );

        String imageUrl = imageService.storeFile(emptyFile);

        assertThat(imageUrl).isNull();
        assertThat(Files.exists(uploadDir)).isFalse();
    }
}

