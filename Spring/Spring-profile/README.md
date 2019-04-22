![](https://github.com/KingCjy/blog/blob/master/Spring/Spring-profile/images/spring-boot-logo.png?raw=true)

# 환경에 맞는 Spring Profile 설정하기

스프링 부트로 서버를 개발하다보면 개발환경과 리얼 환경에서의 profile을 다르게 설정해야하는 경우가 생깁니다. ( 데이터베이스 설정,   외부 연동 url등)
이러한 경우에 스프링에서 지원하는 Spring Profile을 통해 환경에 따라 다른 profile을 설정해줄 수 있습니다.



## Profile 작성하기

먼저 스프링 프로필을 작성해야합니다.

<code>/src/main/resources/develop/application.yaml</code>

```yaml
spring.profile.value: develop
```

<code>/src/main/resources/production/application.yaml</code>

```yaml
spring.profile.value: production
```



이제 스프링에서 @Profile 어노테이션을 사용해 프로필을 추가해줘야합니다.

<code>/src/main/java/com/kingcjy/main/config/profile/ProfileDevelop.java</code>

```java
@Configuration
@Profile(value="develop")
@PropertySource({"classpath:develop/application.properties"})
public class ProfileDevelop {
}
```

<code>/src/main/java/com/kingcjy/main/config/profile/ProfileProduction.java</code>

```java
@Configuration
@Profile(value="production")
@PropertySource({"classpath:production/application.properties"})
public class ProfileDevelop {
}
```

위의 두개의 프로필 클래스를 import 해줘야합니다.

<code>/src/main/java/com/kingcjy/main/config/profile/ProfileConfig.java</code>

```java
@Import({ ProfileDevelop.class, ProfileProduction.class})
@Configuration
public class ProfileConfig {
}
```



## 기본 프로필 설정하기

이제 스프링 부트의 기본 프로필을 설정하기 위해 시작 클래스를 수정합니다.

<code>/src/main/java/com/kingcjy/main/SpringBootApplication.java</code>

```java
@org.springframework.boot.autoconfigure.SpringBootApplication
public class SpringBootApplication {
    public static void main(String[] args) {

        String profile = System.getProperty("spring.profiles.active");
        if(profile == null) {
            System.setProperty("spring.profiles.active", "develop");
        }

        SpringApplication.run(SpringBootApplication.class, args);
    }

}
```

프로필에대한 정보가 없을 시 develop 프로필을 지정합니다.



## 프로필 결과 확인

정상적으로 적용되었는지 확인하기 위해 간단한 컨트롤러를 작성해줍니다.

<code>/src/main/java/com/kingcjy/main/controller/MyController</code>

```java
@Controller
public class MyController {
    @Value("${spring.profile.value}")
    private String profile;

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return new ResponseEntity<>(profile, HttpStatus.OK);
    }
}
```



이제 스프링 부트를 실행한 다음 <code>http://127.0.0.1:8080/ping</code> 으로 접속하면 develop 프로필이 지정됩니다.

```
develop
```

production 프로필을 지정하기 위해서는 스프링 부트를 실행할 때 spring.profiles.active=production 파라미터를 넣어주시면 아래와 같이 production 프로파일이 지정됩니다.

```
production
```



## 테스트 환경에서의 프로필 설정

테스트 환경에서는 테스트 클래스에 <code>@ActiveProfiles(value = { "develop" })</code> 어노테이션을 작성해주시면 원하시는 프로필이 적용된 상태에서 테스트가 진행됩니다.



## 마무리

사용된 모든 코드는 [GITHUB](https://github.com/KingCjy/blog/tree/master/Spring/Spring-profile) 에 있습니다.