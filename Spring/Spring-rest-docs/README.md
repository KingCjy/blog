![](https://github.com/KingCjy/blog/blob/master/Spring/Spring-rest-docs/images/spring-boot-logo.png?raw=true)

## Spring REST Docs

Spring REST Docs는 RESTful 서비스를 문서화 할 수 있게 도와주는 도구입니다. 기본적으로 Asciidoc을 사용하며 작성된 테스트 코드에 의해 html파일을 생성해줍니다.

Swagger같은 도구로 생성하는 문서가 아닌 Test로 자동 생성 된 스니펫과 자신이 원하는 문서를 결합해서 사용 할 수 있습니다.

사용된 모든 코드는 [GITHUB](https://github.com/KingCjy/blog/tree/master/Spring/Spring-rest-docs) 에 있습니다.

## 메이븐 의존성 추가

`pom.xml` 의 `dependencies` 에 추가합니다.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.restdocs</groupId>
    <artifactId>spring-restdocs-mockmvc</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.8.5</version>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
        </plugin>
        <plugin>
            <groupId>org.asciidoctor</groupId>
            <artifactId>asciidoctor-maven-plugin</artifactId>
            <version>1.5.3</version>
            <executions>
                <execution>
                    <id>generate-docs</id>
                    <phase>prepare-package</phase>
                    <goals>
                        <goal>process-asciidoc</goal>
                    </goals>
                    <configuration>
                        <backend>html</backend>
                        <doctype>book</doctype>
                    </configuration>
                </execution>
            </executions>
            <dependencies>
                <dependency>
                    <groupId>org.springframework.restdocs</groupId>
                    <artifactId>spring-restdocs-asciidoctor</artifactId>
                    <version>2.0.2.RELEASE</version>
                </dependency>
            </dependencies>
        </plugin>
        <plugin>
            <artifactId>maven-resources-plugin</artifactId>
            <version>2.7</version>
            <executions>
                <execution>
                    <id>copy-resources</id>
                    <phase>prepare-package</phase>
                    <goals>
                        <goal>copy-resources</goal>
                    </goals>
                    <configuration>
                        <outputDirectory>
                            ${project.build.outputDirectory}/static/docs
                        </outputDirectory>
                        <resources>
                            <resource>
                                <directory>
                                    ${project.build.directory}/generated-docs
                                </directory>
                            </resource>
                        </resources>
                    </configuration>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.1</version>
            <configuration>
                <source>1.8</source>
                <target>1.8</target>
            </configuration>
        </plugin>
    </plugins>
</build>
```



## 코드 작성

간단하게 상품을 추가, 조회하는 RESTful 서비스 입니다.

`/src/main/java/com/kingcjy/main/dto/ProductDto.java`

```java
@Data
public class ProductDto {
    public Integer id;
    public String name;
    public String desc;
    public Integer quantity;
}
```

`/src/main/java/com/kingcjy/main/controller/ProductController.java`

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @PostMapping("")
    public ResponseEntity<?> postProduct(@RequestBody ProductDto productDto) {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<?> getAllProducts(@RequestParam String page,
                                            @RequestParam String size) {
        ProductDto productDto = new ProductDto();
        productDto.setId(1);
        productDto.setName("Spring In Action");
        productDto.setDesc("Spring");
        productDto.setQuantity(10);

        List result = new ArrayList<>();
        result.add(productDto);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Integer id) {
        ProductDto productDto = new ProductDto();
        productDto.setId(id);
        productDto.setName("Spring In Action");
        productDto.setDesc("Spring");
        productDto.setQuantity(10);
        return new ResponseEntity<>(productDto, HttpStatus.OK);
    }
}
```



## Test 작성

### MockMvc, RestDocumentation 세팅

`/src/test/java/com/kingcjy/main/controller/ProductControllerTest.java`

```java
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
public class ProductControllerTest {
    @Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;
    private RestDocumentationResultHandler document;

    @Before
    public void setUp() {
        this.document = document(
                "{class-name}/{method-name}",
                preprocessResponse(prettyPrint())
        );
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
                .apply(documentationConfiguration(this.restDocumentation)
                        .uris().withScheme("https").withHost("kingcjy.com").withPort(443))
                .alwaysDo(document)
                .build();
    }
}
```

* `@Rule
  public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();`
  * 기본적으로 스프링은 JUnit을 사용하기 때문에 JUnitRestDocumentation객체를 생성합니다.
  * 생성자에 String 형식으로 output directory를 지정할 수 있습니다. (기본값은 `target/generated-snippets`)
* RestDocumentationResultHandler객체를 생성합니다.
  * 스니펫 경로를 `{class-name}/{method-name}` 로 설정하여 `target/generated-snippets/product-controller-test/메서드이름` 하위에 스니펫이 생성됩니다.
  * `preprocessResponse(prettyPrint())`를 사용하여 json이 정렬됩니다.
* `uris().withScheme("https").withHost("kingcjy.com").withPort(443))`
  * 스니펫 파일에서 나오는 호스트를 변조해줍니다.

### 테스트 코드 작성

#### 상품등록 API 테스트 작성

```java
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
public class ProductControllerTest {
    ...
    @Test
    public void 상품_등록() throws Exception {
        
        ProductDto productDto = new ProductDto();
        productDto.setName("갤럭시 폴드");
        productDto.setDesc("삼성의 폴더블 스마트폰");
        productDto.setQuantity(10);

        String jsonString = new GsonBuilder().setPrettyPrinting().create().toJson(productDto);
        Map<String, Object> data = new Gson().fromJson(jsonString, Map.class);

        mockMvc.perform(
                post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonString)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document.document(
                        requestFields(
                                fieldWithPath("name").description("상품 이름"),
                                fieldWithPath("desc").description("상품 설명"),
                                fieldWithPath("quantity").type(Integer.class).description("상품 수량")
                        )
                ));
    }
}
```

* `requestFields` 를 사용해서 파라미터 정의를 해줍니다.

#### 상품 조회 API 테스트 작성

```java
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
public class ProductControllerTest {
    ...
    @Test
    public void 상품_조회() throws Exception {
        mockMvc.perform(
                get("/api/products/{id}", 1)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document.document(
                        pathParameters(
                                parameterWithName("id").description("상품 id")
                        ),
                        responseFields(
                                fieldWithPath("id").description("상품 아이디"),
                                fieldWithPath("name").description("상품 이름"),
                                fieldWithPath("desc").description("상품 설명"),
                                fieldWithPath("quantity").type(Integer.class).description("상품 수량")
                        )
                ))
                .andExpect(jsonPath("id", is(notNullValue())))
                .andExpect(jsonPath("name", is(notNullValue())))
                .andExpect(jsonPath("desc", is(notNullValue())))
                .andExpect(jsonPath("quantity", is(notNullValue())));
    }   
}
```

* `pathParameter` 와 API의 결과값인 `responseField` 를 지정해줍니다.
* `andExpect` 를 사용하여 결과값에 대한 테스트를 할 수 있습니다.

#### 상품 리스트 조회 테스트 작성

```java
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestConfig.class)
public class ProductControllerTest {
	...
    @Test
    public void 상품_리스트_조회() throws Exception {
        mockMvc.perform(
                get("/api/products")
                        .param("page", "1")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document.document(
                        requestParameters(
                                parameterWithName("page").description("페이지 번호"),
                                parameterWithName("size").description("페이지 사이즈")
                        ),
                        responseFields(
                                fieldWithPath("[].id").description("상품 아이디"),
                                fieldWithPath("[].name").description("상품 이름"),
                                fieldWithPath("[].desc").description("상품 설명"),
                                fieldWithPath("[].quantity").type(Integer.class).description("상품 수량")
                        )
                ))
                .andExpect(jsonPath("[0].id", is(notNullValue())))
                .andExpect(jsonPath("[0].name", is(notNullValue())))
                .andExpect(jsonPath("[0].desc", is(notNullValue())))
                .andExpect(jsonPath("[0].quantity", is(notNullValue())));
    }
}
```

* 마찬가지로 `requestParameters` 와 결과값인 `responseField` 를 지정합니다.



테스트 코드를 모두 작성한 이후에는 스니펫을 생성해야합니다.

```java
mvn install
```

을 통해 스니펫을 생성합니다.

![](/Users/kingcjy/git/blog/Spring/Spring-rest-docs/images/snippets.png)

스니펫은 작성한 코드에 따라 다르게 생성됩니다.

```
//기본적으로 생성
curl-request.adoc
http-request.adoc
httpie-request.adoc
http-response.adoc
request-body.adoc
response-body.adoc
// 테스트 코드에 따라 생성
response-fields.adoc
request-parameters.adoc
request-parts.adoc
path-parameters.adoc
request-part.adoc
```

이제 실제 문서로 만들기 위해 adoc파일을 작성해야 합니다.

/src/main/asciidoc/api-docs.adoc파일을 생성한 후 아래와 같이 작성합니다.

```adoc
= 상품
== 상품 등록
include::{snippets}/product-controller-test/상품_등록/http-request.adoc[]
include::{snippets}/product-controller-test/상품_등록/request-fields.adoc[]
include::{snippets}/product-controller-test/상품_등록/http-response.adoc[]

== 상품 조회
include::{snippets}/product-controller-test/상품_조회/http-request.adoc[]
include::{snippets}/product-controller-test/상품_조회/path-parameters.adoc[]
include::{snippets}/product-controller-test/상품_조회/http-response.adoc[]
include::{snippets}/product-controller-test/상품_조회/response-fields.adoc[]

== 상품 리스트 조회
include::{snippets}/product-controller-test/상품_리스트_조회/http-request.adoc[]
include::{snippets}/product-controller-test/상품_리스트_조회/request-parameters.adoc[]
include::{snippets}/product-controller-test/상품_리스트_조회/http-response.adoc[]
include::{snippets}/product-controller-test/상품_리스트_조회/response-fields.adoc[]
```



작성 후 실제 문서를 생성하기 위해 다시 `maven install` 을 해줍니다.

```
mvn install
```

`install`이 완료되면 스프링 부트를 실행한 후 http://127.0.0.1:8080/docs/api-docs.html로 접속하면

![](/Users/kingcjy/git/blog/Spring/Spring-rest-docs/images/result.png)

위와 같은 문서를 확인할 수 있습니다.

## 마무리

사용된 모든 코드는 [GITHUB](https://github.com/KingCjy/blog/tree/master/Spring/Spring-rest-docs) 에 있습니다.

참고 링크

* [Spring REST Docs 공식 문서](https://docs.spring.io/spring-restdocs/docs/2.0.3.RELEASE/reference/html5/)

  

