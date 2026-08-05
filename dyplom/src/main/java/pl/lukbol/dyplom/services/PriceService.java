package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.DTOs.price.PriceDTO;
import pl.lukbol.dyplom.DTOs.price.PriceRequestDTO;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.classes.Price;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.PriceRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceService {

    private final PriceRepository priceRepository;

    public List<PriceDTO> getAllPrices() {
        return priceRepository.findAll().stream()
                .map(this::toPriceDTO)
                .toList();
    }

    @Transactional
    public ApiResponseDTO addPrice(PriceRequestDTO priceRequestDTO) {
        Price newPrice = new Price(priceRequestDTO.item(), priceRequestDTO.price());
        priceRepository.save(newPrice);
        return new ApiResponseDTO(Messages.NEW_PRICE_ADDED_MESSAGE);
    }

    @Transactional
    public ApiResponseDTO deletePrice(Long id) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new ApplicationException.PriceNotFoundException(Messages.PRICE_NOT_FOUND));
        priceRepository.delete(price);
        return new ApiResponseDTO(Messages.PRICE_DELETE_MESSAGE);
    }

    @Transactional
    public ApiResponseDTO updatePrice(Long id, PriceRequestDTO priceRequestDTO) {
        Price price = priceRepository.findById(id)
                .orElseThrow(() -> new ApplicationException.PriceNotFoundException(Messages.PRICE_NOT_FOUND));
        price.setItem(priceRequestDTO.item());
        price.setPrice(priceRequestDTO.price());
        priceRepository.save(price);
        return new ApiResponseDTO(Messages.UPDATE_PRICE_MESSAGE);
    }

    private PriceDTO toPriceDTO(Price price) {
        return new PriceDTO(price.getId(), price.getItem(), price.getPrice());
    }
}