package pl.lukbol.dyplom.unitTests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.lukbol.dyplom.DTOs.price.PriceRequestDTO;
import pl.lukbol.dyplom.DTOs.response.ApiResponseDTO;
import pl.lukbol.dyplom.classes.Price;
import pl.lukbol.dyplom.common.Messages;
import pl.lukbol.dyplom.exceptions.ApplicationException;
import pl.lukbol.dyplom.repositories.PriceRepository;
import pl.lukbol.dyplom.services.PriceService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {

    @Mock
    private PriceRepository priceRepository;

    @InjectMocks
    private PriceService priceService;

    private Price price;
    private PriceRequestDTO priceRequestDTO;

    @BeforeEach
    void setUp() {
        price = new Price(1L, "Szycie spodni", "50");
        priceRequestDTO = new PriceRequestDTO("Szycie spodni", "50");
    }

    // getAllPrices

    @Test
    void getAllPrices_shouldReturnAllPrices() {
        when(priceRepository.findAll()).thenReturn(List.of(price));

        List<Price> result = priceService.getAllPrices();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getItem()).isEqualTo("Szycie spodni");
    }

    @Test
    void getAllPrices_shouldReturnEmptyList_whenNoPrices() {
        when(priceRepository.findAll()).thenReturn(List.of());

        List<Price> result = priceService.getAllPrices();

        assertThat(result).isEmpty();
    }

    // addPrice

    @Test
    void addPrice_shouldSavePriceAndReturnSuccessMessage() {
        ApiResponseDTO result = priceService.addPrice(priceRequestDTO);

        verify(priceRepository).save(any(Price.class));
        assertThat(result.message()).isEqualTo(Messages.NEW_PRICE_ADDED_MESSAGE);
    }

    @Test
    void addPrice_shouldSavePriceWithCorrectData() {
        priceService.addPrice(priceRequestDTO);

        verify(priceRepository).save(argThat(saved ->
                saved.getItem().equals("Szycie spodni") &&
                        saved.getPrice().equals("50")
        ));
    }

    // deletePrice

    @Test
    void deletePrice_shouldDeletePriceAndReturnSuccessMessage() {
        when(priceRepository.findById(1L)).thenReturn(Optional.of(price));

        ApiResponseDTO result = priceService.deletePrice(1L);

        verify(priceRepository).delete(price);
        assertThat(result.message()).isEqualTo(Messages.PRICE_DELETE_MESSAGE);
    }

    @Test
    void deletePrice_shouldThrowPriceNotFoundException_whenPriceNotFound() {
        when(priceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.deletePrice(99L))
                .isInstanceOf(ApplicationException.PriceNotFoundException.class)
                .hasMessage(Messages.PRICE_NOT_FOUND);

        verify(priceRepository, never()).delete(any());
    }

    // updatePrice

    @Test
    void updatePrice_shouldUpdatePriceAndReturnSuccessMessage() {
        PriceRequestDTO updateRequest = new PriceRequestDTO("Szycie sukienki", "80");
        when(priceRepository.findById(1L)).thenReturn(Optional.of(price));

        ApiResponseDTO result = priceService.updatePrice(1L, updateRequest);

        assertThat(price.getItem()).isEqualTo("Szycie sukienki");
        assertThat(price.getPrice()).isEqualTo("80");
        verify(priceRepository).save(price);
        assertThat(result.message()).isEqualTo(Messages.UPDATE_PRICE_MESSAGE);
    }

    @Test
    void updatePrice_shouldThrowPriceNotFoundException_whenPriceNotFound() {
        when(priceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> priceService.updatePrice(99L, priceRequestDTO))
                .isInstanceOf(ApplicationException.PriceNotFoundException.class)
                .hasMessage(Messages.PRICE_NOT_FOUND);

        verify(priceRepository, never()).save(any());
    }
}