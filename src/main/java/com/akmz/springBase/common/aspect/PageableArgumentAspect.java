package com.akmz.springBase.common.aspect;

import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class PageableArgumentAspect {

    // Pageable 타입의 인자를 하나 이상 받는 모든 public 메서드에 적용
    @Around("execution(public * *(.., org.springframework.data.domain.Pageable, ..))")
    public Object handlePageableArgument(ProceedingJoinPoint joinPoint) throws Throwable {
        Pageable pageable = null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Pageable) {
                pageable = (Pageable) arg;
                break;
            }
        }

        if (pageable != null) {
            PageHelper.startPage(pageable.getPageNumber() + 1, pageable.getPageSize());
            if (pageable.getSort().isSorted()) {
                String orderBy = pageable.getSort().stream()
                        .map(order -> order.getProperty() + " " + order.getDirection().name())
                        .collect(Collectors.joining(", "));
                PageHelper.orderBy(orderBy);
            }
        }

        try {
            return joinPoint.proceed();
        } finally {
            PageHelper.clearPage();
        }
    }
}