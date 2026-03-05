package myex.shopping;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // 내부적으로 @ComponentScan 포함 : 빈 찾아서 등록해줌. : 기본 스캔 경로 : 현재 패키지와 하위 패키지 (shopping 과
						// 그 하위.)

@OpenAPIDefinition(info = @Info(title = "My Shopping Mall", version = "1.0.0", description = "나의 쇼핑몰 API 문서 - Swagger"), servers = {
		@Server(url = "http://localhost:8080", description = "로컬 개발 서버")
})
public class ShoppingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShoppingApplication.class, args);
	}

}
