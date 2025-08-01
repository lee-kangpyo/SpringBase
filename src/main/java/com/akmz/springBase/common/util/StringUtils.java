package com.akmz.springBase.common.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class StringUtils {

    /**
     * 콤마로 구분된 문자열을 문자열 리스트로 변환합니다.
     * 콤마 앞뒤의 공백은 자동으로 제거됩니다.
     *
     * @param commaSeparatedString 콤마로 구분된 문자열
     * @return 변환된 문자열 리스트. 입력이 null이거나 비어있으면 빈 리스트를 반환합니다.
     */
    public static List<String> fromCommaSeparatedString(String commaSeparatedString) {
        if (commaSeparatedString == null || commaSeparatedString.trim().isEmpty()) {
            return Collections.emptyList();
        }
        // 콤마(,)를 기준으로 분리하되, 각 요소의 앞뒤 공백을 제거하기 위해 정규식을 사용합니다.
        return Arrays.asList(commaSeparatedString.split("\\s*,\\s*"));
    }
}
