package pl.lukbol.dyplom.services;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.DTOs.price.PriceRequestDTO;
import pl.lukbol.dyplom.classes.Price;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.PriceRepository;

import java.util.List;

@Service
public class PriceService {

    private final PriceRepository priceRepository;

    public PriceService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;

    }

    public List<Price> getAllPrices() {
        List<Price> prices = priceRepository.findAll();
        return prices;
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
}
