package pl.lukbol.dyplom.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.DTOs.price.PriceRequestDTO;
import pl.lukbol.dyplom.classes.Price;
import pl.lukbol.dyplom.services.PriceService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PriceController {

    private final PriceService priceService;

    @GetMapping("/prices")
    @ResponseBody
    public ResponseEntity<List<Price>> getAllPrices() {
        return ResponseEntity.ok(priceService.getAllPrices());
    }

    @PostMapping("/add-price")
    public ResponseEntity<ApiResponseDTO> addPrice(@RequestBody PriceRequestDTO priceRequestDTO) {
        ApiResponseDTO response = priceService.addPrice(priceRequestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete-price/{id}")
    public ResponseEntity<ApiResponseDTO> deletePrice (@PathVariable Long id) {
        ApiResponseDTO response = priceService.deletePrice(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/update-price/{id}")
    public ResponseEntity<ApiResponseDTO> updatePrice(@PathVariable Long id, @RequestBody PriceRequestDTO priceRequestDTO) {
        ApiResponseDTO response = priceService.updatePrice(id, priceRequestDTO);
            return ResponseEntity.ok(response);

    }
}
