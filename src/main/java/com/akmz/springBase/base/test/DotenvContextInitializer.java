package com.akmz.springBase.base.test;


import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.HashMap;
import java.util.Map;

public class DotenvContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        try {
            // .env 파일 로드
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // .env 파일이 없어도 예외 발생시키지 않음
                    .load();

            // 로드된 .env 변수들을 Spring Environment의 PropertySource로 추가
            Map<String, Object> dotenvProperties = new HashMap<>();
            dotenv.entries().forEach(entry -> {
                dotenvProperties.put(entry.getKey(), entry.getValue());
                // 시스템 프로퍼티로도 설정하여 @Value("${...}")가 인식하도록 함
                System.setProperty(entry.getKey(), entry.getValue());
            });

            // 환경에 PropertySource 추가
            // 이렇게 하면 스프링이 .env 변수를 @Value, ${...} 등으로 접근할 수 있게 됩니다.
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            MutablePropertySources propertySources = environment.getPropertySources();
            propertySources.addFirst(new MapPropertySource("dotenv", dotenvProperties));

            System.out.println("DotenvContextInitializer: .env variables loaded for test context.");

        } catch (Exception e) {
            System.err.println("DotenvContextInitializer: Failed to load .env variables: " + e.getMessage());
            // 테스트가 실패하도록 할지, 아니면 무시하고 진행할지는 애플리케이션 정책에 따라 다릅니다.
            // 여기서는 경고만 하고 진행하도록 하겠습니다.
        }
    }
}
