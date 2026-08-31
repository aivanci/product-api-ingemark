package hr.ingemark.assignment.productapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import hr.ingemark.assignment.productapi.dto.ProductRequest;
import hr.ingemark.assignment.productapi.dto.ProductResponse;
import hr.ingemark.assignment.productapi.exception.DuplicateProductCodeException;
import hr.ingemark.assignment.productapi.exception.ProductNotFoundException;
import hr.ingemark.assignment.productapi.service.ProductService;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String VALID_CODE = "ABCDEFGHIJ";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void createProduct_returns201WithLocationAndComputedUsdPrice() throws Exception {
        ProductResponse response = new ProductResponse(
                1L, VALID_CODE, "Widget", new BigDecimal("100.00"), new BigDecimal("116.45"), true);
        given(productService.createProduct(any(ProductRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/products/1"))
                .andExpect(jsonPath("$.code").value(VALID_CODE))
                .andExpect(jsonPath("$.price_eur").value(100.00))
                .andExpect(jsonPath("$.price_usd").value(116.45))
                .andExpect(jsonPath("$.is_available").value(true));
    }

    @Test
    void createProduct_returns400WithWireFieldNamesWhenRequestIsInvalid() throws Exception {
        String requestBody = """
                {"code":"SHORT","name":"Widget","price_eur":-1,"is_available":true}
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.code").exists())
                .andExpect(jsonPath("$.fieldErrors.price_eur").exists());
    }

    @Test
    void createProduct_returns409WhenCodeAlreadyExists() throws Exception {
        given(productService.createProduct(any(ProductRequest.class)))
                .willThrow(new DuplicateProductCodeException("Product with code '%s' already exists".formatted(VALID_CODE)));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void getProduct_returns200WhenFound() throws Exception {
        ProductResponse response = new ProductResponse(
                1L, VALID_CODE, "Widget", new BigDecimal("100.00"), new BigDecimal("116.45"), true);
        given(productService.getProduct(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(VALID_CODE));
    }

    @Test
    void getProduct_returns404WhenNotFound() throws Exception {
        given(productService.getProduct(eq(99L)))
                .willThrow(new ProductNotFoundException("Product with id 99 not found"));

        mockMvc.perform(get("/api/v1/products/{id}", 99L))
                .andExpect(status().isNotFound());
    }

    private String validRequestJson() {
        return """
                {"code":"%s","name":"Widget","price_eur":100.00,"is_available":true}
                """.formatted(VALID_CODE);
    }
}
