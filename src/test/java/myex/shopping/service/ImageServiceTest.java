package myex.shopping.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private ImageService imageService;

    @BeforeEach
    void setUp() {
        imageService = new ImageService(s3Client, s3Presigner);
        ReflectionTestUtils.setField(imageService, "bucket", "test-bucket");
        ReflectionTestUtils.setField(imageService, "presignedUrlExpirationMinutes", 60L);
    }

    @Test
    @DisplayName("이미지를 S3에 업로드하고 S3 key(상대경로)를 반환한다")
    void storeFileUploadsToS3AndReturnsKey() throws IOException {
        // given
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        MockMultipartFile multipartFile = new MockMultipartFile(
                "imageFile",
                "test-image.png",
                "image/png",
                "image-content".getBytes()
        );

        // when
        String key = imageService.storeFile(multipartFile);

        // then - DB에 저장되는 값은 상대경로(S3 key)
        assertThat(key).startsWith("images/").endsWith(".png");
        assertThat(key).doesNotContain("https://"); // 절대 URL이 아님을 확인

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).startsWith("images/").endsWith(".png");
        assertThat(capturedRequest.contentType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("빈 파일이면 S3에 업로드하지 않고 null을 반환한다")
    void storeFileReturnsNullForEmptyFile() throws IOException {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "imageFile",
                "empty.png",
                "image/png",
                new byte[0]
        );

        String key = imageService.storeFile(emptyFile);

        assertThat(key).isNull();
        verifyNoInteractions(s3Client);
    }

    @Test
    @DisplayName("S3 key로 Pre-signed URL을 생성한다")
    void generatePresignedUrlReturnsValidUrl() throws Exception {
        // given
        String key = "images/test-uuid.png";
        String expectedUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/images/test-uuid.png?X-Amz-Algorithm=AWS4-HMAC-SHA256";

        PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
        when(presignedRequest.url()).thenReturn(URI.create(expectedUrl).toURL());
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                .thenReturn(presignedRequest);

        // when
        String url = imageService.generatePresignedUrl(key);

        // then
        assertThat(url).startsWith("https://");
        assertThat(url).contains("test-bucket");
        assertThat(url).contains("images/test-uuid.png");
        verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("null이나 빈 key이면 Pre-signed URL을 생성하지 않는다")
    void generatePresignedUrlReturnsNullForNullOrBlankKey() {
        assertThat(imageService.generatePresignedUrl(null)).isNull();
        assertThat(imageService.generatePresignedUrl("")).isNull();
        assertThat(imageService.generatePresignedUrl("   ")).isNull();
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("S3에서 이미지를 삭제한다")
    void deleteFileDeletesFromS3() {
        String key = "images/test-uuid.png";

        imageService.deleteFile(key);

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).isEqualTo("images/test-uuid.png");
    }

    @Test
    @DisplayName("null이나 빈 key이면 삭제를 시도하지 않는다")
    void deleteFileDoesNothingForNullOrBlankKey() {
        imageService.deleteFile(null);
        imageService.deleteFile("");
        imageService.deleteFile("   ");

        verifyNoInteractions(s3Client);
    }
}

