package com.akmz.springBase.common.util;

import com.github.pagehelper.PageInfo;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PageUtils {

    /**
     * PageHelper의 PageInfo와 Spring Data Pageable을 사용하여 Spring Data Page 객체를 생성.
     * 엔티티 리스트를 DTO 리스트로 변환하는 기능을 포함.
     *
     * @param entityList PageHelper가 페이징 처리한 엔티티 리스트
     * @param pageable Spring Data Pageable 객체
     * @param converter 엔티티를 DTO로 변환하는 함수
     * @param <E> 엔티티 타입
     * @param <D> DTO 타입
     * @return Spring Data Page 객체
     */
    public static <E, D> Page<D> convertPage(List<E> entityList, Pageable pageable, Function<E, D> converter) {
        PageInfo<E> pageInfo = new PageInfo<>(entityList);
        List<D> content = pageInfo.getList().stream()
                .map(converter)
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageInfo.getTotal());
    }

    /**
     * PageHelper의 PageInfo와 Spring Data Pageable을 사용하여 Spring Data Page 객체를 생성.
     * 엔티티 리스트를 DTO 리스트로 변환하는 기능이 없는 오버로드 메서드 (엔티티와 DTO가 동일한 경우 등).
     *
     * @param entityList PageHelper가 페이징 처리한 엔티티 리스트
     * @param pageable Spring Data Pageable 객체
     * @param <E> 엔티티 타입
     * @return Spring Data Page 객체
     */
    public static <E> Page<E> convertPage(List<E> entityList, Pageable pageable) {
        PageInfo<E> pageInfo = new PageInfo<>(entityList);
        return new PageImpl<>(pageInfo.getList(), pageable, pageInfo.getTotal());
    }

    /**
     * PageHelper의 PageInfo와 Spring Data Pageable을 사용하여 Spring Data Page 객체를 생성.
     * DTO 클래스 타입을 받아 리플렉션을 통해 엔티티를 DTO로 자동 변환.
     *
     * @param entityList PageHelper가 페이징 처리한 엔티티 리스트
     * @param pageable Spring Data Pageable 객체
     * @param targetClass 변환될 DTO 클래스 타입
     * @param <E> 엔티티 타입
     * @param <D> DTO 타입
     * @return Spring Data Page 객체
     */
    public static <E, D> Page<D> convertPage(List<E> entityList, Pageable pageable, Class<D> targetClass) {
        PageInfo<E> pageInfo = new PageInfo<>(entityList);
        List<D> content = pageInfo.getList().stream()
                .map(entity -> {
                    try {
                        D dto = targetClass.getDeclaredConstructor().newInstance();
                        BeanUtils.copyProperties(entity, dto);
                        return dto;
                    } catch (Exception e) {
                        throw new RuntimeException("Error converting entity to DTO using reflection: " + e.getMessage(), e);
                    }
                })
                .collect(Collectors.toList());
        return new PageImpl<>(content, pageable, pageInfo.getTotal());
    }
}