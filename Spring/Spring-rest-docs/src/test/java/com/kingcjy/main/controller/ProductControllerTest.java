package com.kingcjy.main.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.kingcjy.main.TestConfig;
import com.kingcjy.main.dto.ProductDto;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Map;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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