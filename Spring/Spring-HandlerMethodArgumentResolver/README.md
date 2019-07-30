## 1. HandlerMethodArgumentResolver 이란?

HandlerMethodArgumentResolver은 컨트롤러 메서드에서 특정 조건에 맞는 파라미터가 있을 때 원하는 값을 바인딩해주는 인터페이스입니다.

스프링에서는 Controller에서 @RequestBody 어노테이션을 사용해 Request의 Body 값을 받아올 때, 
@PathVariable 어노테이션을 사용해 Request 의 Path Parameter 값을 받아올 때 이 HandlerMethodArgumentResolver를 사용해서 값을 받아옵니다.

## 2. HandlerMethodArgument 사용하기

### 객체를 Controller 파라미터에 바인딩하기

컨트롤러에 특정한 Person이라는 객체가 파라미터로 존재 시 원하는 값을 바인딩하는 예제를 작성합니다.

#### 1. 어노테이션 작성
`com.kingcjy.demo.annotation.RequestParameter`

```java
package com.kingcjy.demo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomResolver {
}
```

#### 2. Person 객체 작성
`com.kingcjy.demo.person.Person`
```java
@Getter
@Setter
public class Person {

    private String name;
    private Integer age;
    private String gender;
    
}
```

#### 3. 컨트롤러 작성
`com.kingcjy.demo.controller.TestController`

```java
@RestController
public class TestController {

    @GetMapping("/")
    public Person test(@CustomResolver Person person) {
        return person;
    }
}

```

#### 4. resolver 작성
이제 `HandlerMethodArgumentResolver`를 상속받은 `CustomHandlerMethodArgumentResolver`을 작성해줍니다.

`HandlerMethodArgumentResolver`를 상속받은 객체는
```java
boolean supportsParameter(MethodParameter var1);
Object resolveArgument(MethodParameter var1, @Nullable ModelAndViewContainer var2, NativeWebRequest var3, @Nullable WebDataBinderFactory var4) throws Exception;
```
두개의 메서드를 구현해야 합니다.

* `supportsParameter` 메서드는 현재 파라미터를 resolver이 지원하는지에 대한 boolean을 리턴합니다.
* `resolveArgument` 메서드는 실제로 바인딩을 할 객체를 리턴합니다.

`com.kingcjy.demo.resolver.CustomHandlerMethodArgumentResolver`
```java
@Component
public class CustomHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {
    
    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(Person.class);
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, ModelAndViewContainer modelAndViewContainer, NativeWebRequest nativeWebRequest, WebDataBinderFactory webDataBinderFactory) throws Exception {

        Person person = new Person();
        person.setName("KingCjy");
        person.setAge(21);
        person.setGender("male");

        return person;
    }
}
```

* `supportsParameter` 메서드는 현재 파라미터의 타입이 Person일 경우에 true를 리턴합니다.
* `resolveArgument` 메서드는 실제 바인딩할 Person 객체를 만들어서 리턴합니다.


#### 5. resolver 등록
마지막으로 작성한 CustomHandlerMethodArgumentResolver를 스프링에 등록합니다
`com.kingcjy.demo.config.WebConfig`

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private CustomHandlerMethodArgumentResolver customHandlerMethodArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(customHandlerMethodArgumentResolver);
    }
}
```
#### 6. 결과 확인

스프링을 실행시킨 후 localhost:8080 에 접속하면 아래와 같은 결과가 나옵니다.
`{"name":"KingCjy","age":21,"gender":"male"}`

사용된 모든 코드는 [GITHUB](https://github.com/KingCjy/blog/tree/master/Spring/Spring-HandlerMethodArgumentResolver) 에 있습니다.

## 2. HandlerMethodArgumentResolver 작동원리

Spring은 Request 요청을 Controller의 method를 wrapping한 `InvocableHandlerMethod`의 `invokeForRequest` 메서드로 처리합니다.

### 1. InvocableHandlerMethod
`org.springframework.web.method.support.InvocableHandlerMethod`
```java
@Nullable
public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainerm, mavContainer, Object... providedArgs) throws Exception {
	Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);
    if (logger.isTraceEnabled()) {
      logger.trace("Arguments: " + Arrays.toString(args));
    }
    return doInvoke(args);
}
```
여기서 args를 가져오는 `getMethodArgumentValues` 메서드를 살펴보면

```java
protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {

    MethodParameter[] parameters = getMethodParameters();
    if (ObjectUtils.isEmpty(parameters)) {
        return EMPTY_ARGS;
    }

  	// 결과 파라미터 들을 담을 Object 배열입니다.
    Object[] args = new Object[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
        MethodParameter parameter = parameters[i];
        parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);
        args[i] = findProvidedArgument(parameter, providedArgs);
        if (args[i] != null) {
            continue;
        }
      	// resolver이 지원하지 않는 파라미터면 예외처리
        if (!this.resolvers.supportsParameter(parameter)) {
            throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));
        }
        try {
          	// resolver에서 값을 바인딩
            args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);
        }
        catch (Exception ex) {
            // Leave stack trace for later, exception may actually be resolved and handled...
            if (logger.isDebugEnabled()) {
                String exMsg = ex.getMessage();
                if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {
                    logger.debug(formatArgumentError(parameter, exMsg));
                }
            }
            throw ex;
        }
    }
    return args;
}   
```
위와 같은 코드가 나옵니다. 

`this.resolvers` 를 살펴보면

```java
private HandlerMethodArgumentResolverComposite resolvers = new HandlerMethodArgumentResolverComposite();
```

`HandlerMethodArgumentResolverComposite`타입의 객체인걸 할 수 있습니다.

### 2. HandlerMethodArgumentResolverComposite
`HandlerMethodArgumentResolverComposite`의 소스를 들어가보면

```java
public class HandlerMethodArgumentResolverComposite implements HandlerMethodArgumentResolver {

    ...
    private final List<HandlerMethodArgumentResolver> argumentResolvers = new LinkedList<>();

    private final Map<MethodParameter, HandlerMethodArgumentResolver> argumentResolverCache =
            new ConcurrentHashMap<>(256);

    @Override
    @Nullable
    public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

        HandlerMethodArgumentResolver resolver = getArgumentResolver(parameter);
        if (resolver == null) {
            throw new IllegalArgumentException(
                    "Unsupported parameter type [" + parameter.getParameterType().getName() + "]." +
                            " supportsParameter should be called first.");
        }
        return resolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }

    @Nullable
    private HandlerMethodArgumentResolver getArgumentResolver(MethodParameter parameter) {
        HandlerMethodArgumentResolver result = this.argumentResolverCache.get(parameter);
        if (result == null) {
            for (HandlerMethodArgumentResolver methodArgumentResolver : this.argumentResolvers) {
                if (methodArgumentResolver.supportsParameter(parameter)) {
                    result = methodArgumentResolver;
                    this.argumentResolverCache.put(parameter, result);
                    break;
                }
            }
        }
        return result;
    }
}
```
`HandlerMethodArgumentResolver`를 상속받은 객체인걸 확인할 수 있습니다.
`HandlerMethodArgumentResolverComposite`은 다른 모든 `HandlerMethodArgumentResolver`를 상속받은 객체를 가지고 루프를 돌며 지원하는 파라미터인지 확인을 하고 결과를 리턴합니다.


사용된 모든 코드는 [GITHUB](https://github.com/KingCjy/blog/tree/master/Spring/Spring-HandlerMethodArgumentResolver) 에 있습니다.

