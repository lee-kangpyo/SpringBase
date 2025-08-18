package com.akmz.springBase;

import com.akmz.springBase.common.test.DotenvContextInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

@SpringBootTest
@ContextConfiguration(initializers = DotenvContextInitializer.class)
@Sql(scripts = {"classpath:sql/clean_all_tables.sql", "classpath:schema.sql", "classpath:data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
class SpringBaseApplicationTests {

	// 스프링 구동 테스트용 에러 안나면 정상적으로 빌드됨
	@Test
	void contextLoads() {
	}

}
