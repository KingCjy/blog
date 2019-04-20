## Spring Boot, JPA DB Replication 구현하기

![](/Users/kingcjy/git/blog/Spring/Spring-db-replication/images/spring-boot-logo.png)

### DB Replication 이란?

데이터베이스 이중화 방식 중 하나로 하나의 Master DB와 여러대의 Slave DB로 구성한다.

Master DB에 데이터의 변경이 감지되면 Master DB의 로그를 기반으로 Slave DB에 복제한다.

Master DB에는 데이터의 변경이 필요한  INSERT, UPDATE, DELETE 등의 쿼리가 필요할때 사용하고, Slave DB에는 Select문이 필요할때 사용한다.



*** 데이터베이스 세팅은 이 글에서 다루지 않습니다.

### 목표

Master DB와 Slave DB를 나눠서 구성하고

@Transaction의 readOnly속성을 사용하여 true일 시 Slave DB, false일 시 Master DB를 사용한다.

### 구성

- Spring Boot 2.1.4
- JPA
- Aws Rds Mysql

## 메이븐 의존성 추가

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-entitymanager</artifactId>
    <version>5.3.7.Final</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```



### 스프링 설정파일 작성

jpa 기본 설정입니다.

<pre><code>src/main/resources/application.yml</code></pre>

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: none
      naming:
        physical-strategy: org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl
      generate-ddl: false
    show-sql: true
    database-platform: org.hibernate.dialect.MySQL5InnoDBDialect
    properties:
      hibernate:
        enable_lazy_load_no_trans: true
        format_sql: true
    open-in-view: false
  main:
    allow-bean-definition-overriding: true
```

아래에 Database 설정을 추가해줍니다.

<pre><code>src/main/resources/application.yml</code></code>

```yaml
datasource:
  url: jdbc:mysql://replication.c9t6dmtnqwlu.ap-northeast-2.rds.amazonaws.com:3306/replication?useSSL=false&serverTimezone=UTC&useCursors=false&sendStringParametersAsUnicode=false&characterEncoding=UTF8
  slave-list:
    - name: slave_1
      url: jdbc:mysql://replication-slave1.c9t6dmtnqwlu.ap-northeast-2.rds.amazonaws.com:3306/replication?useSSL=false&serverTimezone=UTC&useCursors=false&sendStringParametersAsUnicode=false&characterEncoding=UTF8
    - name: slave_2
      url: jdbc:mysql://replication-slave2.c9t6dmtnqwlu.ap-northeast-2.rds.amazonaws.com:3306/replication?useSSL=false&serverTimezone=UTC&useCursors=false&sendStringParametersAsUnicode=false&characterEncoding=UTF8
    - name: slave_3
      url: jdbc:mysql://replication-slave3.c9t6dmtnqwlu.ap-northeast-2.rds.amazonaws.com:3306/replication?useSSL=false&serverTimezone=UTC&useCursors=false&sendStringParametersAsUnicode=false&characterEncoding=UTF8
  username: username
  password: password
```

### 코드 작성

가장 먼저 우리는 DataSource를 직접 설정해야하기 때문에 Spring에서 DataSourceAutoConfiguration 클래스를 제외해야합니다.

<pre><code>/src/main/com/kingcjy/replication/ReplicationApplication.java</code></pre>

```java
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ReplicationApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReplicationApplication.class, args);
    }
}
```

DB의 설정파일을 가져올 DatabaseProperty클래스를 만들어줍니다.

<pre><code>src/main/com/kingcjy/replication/config/DatabaseProperty.java</code></pre>

```java
@Getter
@Setter
@Component
@ConfigurationProperties("datasource")
public class DatabaseProperty {
    private String url;
    private List<Slave> slaveList;

    private String username;
    private String password;

    @Getter
    @Setter
    public static class Slave {
        private String name;
        private String url;
    }
}
```

여러개의 DataSource를 로드밸런싱을 하기 위해 CircurlarList 클래스를 만들어줍니다.

<pre><code>/src/main/kingcjy/replication/util/CircularList</code></pre>

```java
public class CircularList<T> {
    private List<T> list;
    private Integer counter = 0;
  
    public CircularList(List<T> list) {
        this.list = list;
    }
    public T getOne() {
        if(counter + 1 >= list.size()) {
            counter = -1;
        }
        return list.get(++counter);
    }
}
```

여러개의 DataSource를 묶고 필요에 따라 분기처리를 하기 위해 AbstractRoutingDataSource클래스를 사용합니다.

여러대의 Slave DB를 순서대로 사용하기 위해 CircularList에 Slave 데이터베이스의 키를 추가합니다.

determineCurrentLookupKey 메서드에서 현재 트랜잭션이 readOnly일 시 slave db로, 아닐 시 master db의 DataSource의 키를 리턴한다.

<pre><code>/src/main/com/kingcjy/replication/config/ReplicationRoutingDataSource</code></pre>

```java
public class ReplicationRoutingDataSource extends AbstractRoutingDataSource {

    private CircularList<String> dataSourceNameList;

    @Override
    public void setTargetDataSources(Map<Object, Object> targetDataSources) {
        super.setTargetDataSources(targetDataSources);

        dataSourceNameList = new CircularList<>(
                targetDataSources.keySet()
                        .stream()
                        .filter(key -> key.toString().contains("slave"))
                        .map(key -> key.toString())
                        .collect(Collectors.toList())
        );
    }
    @Override
    protected Object determineCurrentLookupKey() {
        boolean isReadOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        if(isReadOnly) {
            return dataSourceNameList.getOne();
        } else {
            return "master";
        }
    }
}
```

이제 최종적으로 DataSource, TransactionManager, EntityManagerFactory를 설정해야합니다.

<pre><code>src/main/com/kingcjy/replication/config/DatabaseConfig</code></pre>

가장 먼저 DataSource를 설정합니다.

```java
@Configuration
public class DatabaseConfig {

    @Autowired
    private DatabaseProperty databaseProperty;

    public DataSource createDataSource(String url) {
        SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
        dataSource.setUrl(url);
        dataSource.setDriverClass(com.mysql.cj.jdbc.Driver.class);
        dataSource.setUsername(databaseProperty.getUsername());
        dataSource.setPassword(databaseProperty.getPassword());

        return dataSource;
    }
    @Bean
    public DataSource routingDataSource() {
        ReplicationRoutingDataSource replicationRoutingDataSource = new ReplicationRoutingDataSource();

            DataSource master = createDataSource(databaseProperty.getUrl());

            Map<Object, Object> dataSourceMap = new LinkedHashMap<>();
            dataSourceMap.put("master", master);

            databaseProperty.getSlaveList().forEach(slave -> {
                dataSourceMap.put(slave.getName(), createDataSource(slave.getUrl()));
            });

            replicationRoutingDataSource.setTargetDataSources(dataSourceMap);
            replicationRoutingDataSource.setDefaultTargetDataSource(master);
            return replicationRoutingDataSource;
        }

    @Bean
    public DataSource dataSource() {
        return new LazyConnectionDataSourceProxy(routingDataSource());
    }
}
```

아까 만들었던 ReplicationRoutingDataSource클래스에 Master 데이터베이스와 Slave 데이터베이스를 추가해줍니다.

LazyConnectionDataSourceProxy를 사용하면 실제 쿼리가 실행될 때 Connection을 가져옵니다. 

TransactionSynchronizationManager가 현재 트랜잭션의 상태값을 읽어올 수 있지만 실제 트랜잭션 동기화 시점과 Connection이 연결되는 시점이 다르기 때문에 LazyConnectionDataSourceProxy를 사용해 트랜잭션 실행시에 Connection객체를 가져옵니다.

이후에 Jpa에서 사용할 EntityManagerFactory와 TransactionManager를 설정해줍니다.

```java
@Configuration
public class DatabaseConfig {
  
		...
  
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactoryBean.setDataSource(dataSource());
        entityManagerFactoryBean.setPackagesToScan("com.kingcjy.replication");
        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerFactoryBean.setJpaVendorAdapter(vendorAdapter);

        return entityManagerFactoryBean;
    }
    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager tm = new JpaTransactionManager();
        tm.setEntityManagerFactory(entityManagerFactory);
        return tm;
    }
}
```

이렇게하면 세팅이 끝났습니다. 실제로 작동하는지 테스트하기 위해 Controller, Service, repository, entity를 작성합니다.



### 테스트용 코드 작성

<pre><code>/src/main/com/kingcjy/replication/entity/Product</code></pre>

```java
@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String contents;

    @Builder
    public Product(String title, String contents) {
        this.title = title;
        this.contents = contents;
    }
}
```

<pre><code>src/main/kingcjy/replication/entity/ProductRepository</code></pre>

```java
public interface ProductRepository extends JpaRepository<Product, Long> {}
```

<pre><code>src/main/com/kingcjy/replication/controller/ProductController</code></pre>

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping("")
    public ResponseEntity<?> getProducts() {
        List<Product> productList = productService.getProducts();
        return new ResponseEntity<>(productList, HttpStatus.OK);
    }
    @GetMapping("/master")
    public ResponseEntity<?> getProductsFromMaster() {
        List<Product> productList = productService.getProductsMaster();
        return new ResponseEntity<>(productList, HttpStatus.OK);
    }
}
```

<pre><code>src/main/kingcjy/replication/service/ProductService</code></pre>

```java
@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> getProducts() {
        return productRepository.findAll();
    }
    @Transactional
    public List<Product> getProductsMaster() {
        return productRepository.findAll();
    }
}
```

실제로 Master DB, Slave DB로 쿼리가 날아가는지 확인하기 위해 application.yml에 아래의 코드를 추가합니다.

```yaml
logging:
  level:
    org.springframework.jdbc.datasource.SimpleDriverDataSource: DEBUG
    org.hibernate.SQL: DEBUG
```

DB에 product 테이블을 생성하고 기본 데이터를 넣어줍니다.

```sql
CREATE TABLE `product` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL,
  `contents` varchar(500) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

insert into `product` (title, contents) values ('상품1', '상품1입니다'), ('상품2', '상품2입니다'), ('상품3', '상품3입니다');
```

### 작동 확인

서버를 실행 후 /api/product 에 get 요청을 하면

```json
[
    {
        "id": 1,
        "title": "상품1",
        "contents": "상품1입니다"
    },
    {
        "id": 2,
        "title": "상품2",
        "contents": "상품2입니다"
    },
    {
        "id": 3,
        "title": "상품3",
        "contents": "상품3입니다"
    }
]
```

위와 같은 결과값이 나온다.

SimpleDriverDataSource의 로그를 확인해보면

```Creating new JDBC Driver Connection to [jdbc:mysql://replication-slave2.c9t6dmtnqwlu...```

이렇게 Slave DB를 사용한다. 계속 요청을 하면 Slave DB 1, 2, 3 을 순차적으로 사용한다.

/api/product/master에 요청을 보내면

```Creating new JDBC Driver Connection to [jdbc:mysql://replication.c9t6dmtnqwlu...```

이렇게 Master DB를 사용한다.



### 마무리

모든 소스는 [GITHUB](<https://github.com/KingCjy/blog/tree/master/Spring/Spring-db-replication>) 에 있습니다.