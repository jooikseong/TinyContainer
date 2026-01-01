package com.naver.chapter13jsonserializer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

// 테스트용 도메인 객체
class User {
    private Long id;
    private String name;
    private String email;
    private boolean active;

    public User(Long id, String name, String email, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.active = active;
    }
}

// 핵심: Tiny JSON 직렬화기
class TinyJsonSerializer {

    public static String toJson(Object obj) {
        try {
            Class<?> clazz = obj.getClass();
            Field[] fields = clazz.getDeclaredFields();
            List<String> jsonElemets = new ArrayList<>();

            for (Field field : fields) {
                field.setAccessible(true);

                String name = field.getName();
                Object value = field.get(obj);

                // 값을 JSON 타입에 맞게 포맷팅
                jsonElemets.add("\"" + name + "\":" + formatValue(value));
            }

            return "{" + String.join(",", jsonElemets) + "}";

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String formatValue(Object value) {
        if(value == null)  return "null";

        if(value instanceof String) {return "\"" + value + "\"";}
        else if(value instanceof Boolean) {return "\"" + value.toString() + "\""; }

        return "\"" + value.toString() + "\"";
    }
}

// MVC 컨트롤러와 결합 예시
class MyRestController {
    // 이 메서드가 실행되면 결과가 JSON으로 변환되어 나가야 함 (Jackson 라이브러리의 원리)
    public String getUser() {
        User user = new User(1L, "Tiny Spring", "Tiny Spring", true);
        System.out.println("[System] 객체를 JSON으로 변환합니다...");
        return TinyJsonSerializer.toJson(user);
    }
}


public class TinyJsonMain {
    public static void main(String[] args) {
        MyRestController myRestController = new MyRestController();

        String jsonResponse = myRestController.getUser();

        System.out.println("\n--- 최종 응답 데이터 (JSON) ---");
        System.out.println(jsonResponse);
    }
}
