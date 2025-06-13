package com.akmz.springBase;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = {"com.akmz.springBase.base.mapper", "com.akmz.springBase.mapper"})
public class SpringBaseApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBaseApplication.class, args);
	}

}
