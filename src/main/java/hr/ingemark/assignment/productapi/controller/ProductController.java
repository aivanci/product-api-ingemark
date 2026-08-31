package hr.ingemark.assignment.productapi.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import hr.ingemark.assignment.productapi.dto.ProductRequest;
import hr.ingemark.assignment.productapi.dto.ProductResponse;
import hr.ingemark.assignment.productapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Product catalog management")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Create a product",
            description = "price_usd is computed server-side from price_eur and the current HNB "
                    + "USD middle rate; it cannot be supplied by the client.")
    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.created(locationOf(response)).body(response);
    }

    private URI locationOf(ProductResponse response) {
        return URI.create("/api/v1/products/" + response.id());
    }
}
