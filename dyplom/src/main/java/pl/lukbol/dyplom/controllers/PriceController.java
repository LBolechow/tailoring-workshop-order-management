package pl.lukbol.dyplom.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.DTOs.price.PriceDTO;
import pl.lukbol.dyplom.DTOs.price.PriceRequestDTO;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.services.PriceService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @GetMapping("/prices")
    public ResponseEntity<List<PriceDTO>> getAllPrices() {
        return ResponseEntity.ok(priceService.getAllPrices());
    }

    @PostMapping("/add-price")
    public ResponseEntity<ApiResponseDTO> addPrice(@RequestBody PriceRequestDTO priceRequestDTO) {
        return ResponseEntity.ok(priceService.addPrice(priceRequestDTO));
    }

    @DeleteMapping("/delete-price/{id}")
    public ResponseEntity<ApiResponseDTO> deletePrice(@PathVariable Long id) {
        return ResponseEntity.ok(priceService.deletePrice(id));
    }

    @PutMapping("/update-price/{id}")
    public ResponseEntity<ApiResponseDTO> updatePrice(@PathVariable Long id,
                                                      @RequestBody PriceRequestDTO priceRequestDTO) {
        return ResponseEntity.ok(priceService.updatePrice(id, priceRequestDTO));
    }
}