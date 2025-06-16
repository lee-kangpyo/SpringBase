package com.akmz.springBase;

import com.akmz.springBase.base.test.DotenvContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = DotenvContextInitializer.class)
class SpringBaseApplicationTests {

	// 스프링 구동 테스트용 에러 안나면 정상적으로 빌드됨
	@Test
	void contextLoads() {
	}

}
