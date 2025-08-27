package com.akmz.springBase;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan(basePackages = {"com.akmz.springBase.auth.mapper", "com.akmz.springBase.admin.mapper", "com.akmz.springBase.attach.mapper"})
@EnableScheduling
public class SpringBaseApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.load();
		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
		});

//		System.out.println("====================================================");
//		System.out.println("Default Charset: " + java.nio.charset.Charset.defaultCharset());
//		System.out.println("file.encoding System Property: " + System.getProperty("file.encoding"));
//		System.out.println("====================================================");

		SpringApplication.run(SpringBaseApplication.class, args);
	}

}
