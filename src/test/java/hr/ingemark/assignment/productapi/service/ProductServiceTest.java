package hr.ingemark.assignment.productapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import hr.ingemark.assignment.productapi.dto.ProductRequest;
import hr.ingemark.assignment.productapi.dto.ProductResponse;
import hr.ingemark.assignment.productapi.exception.DuplicateProductCodeException;
import hr.ingemark.assignment.productapi.model.ProductEntity;
import hr.ingemark.assignment.productapi.repo.ProductRepository;
import hr.ingemark.assignment.productapi.util.ProductMapper;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final String VALID_CODE = "ABCDEFGHIJ";

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    private final ProductMapper productMapper = Mappers.getMapper(ProductMapper.class);

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository, exchangeRateService, productMapper);
    }

    @Test
    void createProduct_savesProductWithServerCalculatedUsdPrice() {
        ProductRequest request = new ProductRequest(VALID_CODE, "Widget", new BigDecimal("100.00"), true);
        given(productRepository.existsByCode(VALID_CODE)).willReturn(false);
        given(exchangeRateService.convertEurToUsd(new BigDecimal("100.00"))).willReturn(new BigDecimal("108.50"));
        given(productRepository.save(any(ProductEntity.class))).willAnswer(this::withGeneratedId);

        ProductResponse response = productService.createProduct(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.code()).isEqualTo(VALID_CODE);
        assertThat(response.priceEur()).isEqualByComparingTo("100.00");
        assertThat(response.priceUsd()).isEqualByComparingTo("108.50");
        assertThat(response.available()).isTrue();
        verify(productRepository).save(any(ProductEntity.class));
    }

    @Test
    void createProduct_throwsWhenCodeAlreadyExists() {
        ProductRequest request = new ProductRequest(VALID_CODE, "Widget", new BigDecimal("100.00"), true);
        given(productRepository.existsByCode(VALID_CODE)).willReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateProductCodeException.class);

        verify(productRepository, never()).save(any());
    }

    private ProductEntity withGeneratedId(InvocationOnMock invocation) {
        ProductEntity product = invocation.getArgument(0);
        ReflectionTestUtils.setField(product, "id", 1L);
        return product;
    }
}
