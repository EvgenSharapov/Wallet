package org.example.wallet.controller;

import org.example.wallet.dto.WalletRequest;
import org.example.wallet.dto.WalletResponse;
import org.example.wallet.exceptions.WalletNotFoundException;
import org.example.wallet.model.OperationType;
import org.example.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private WalletService walletService;

    private final UUID testWalletId = UUID.randomUUID();
    private final BigDecimal testAmount = new BigDecimal("2000.00");


    @Test
    void processTransaction_Deposit_ReturnsOk() throws Exception {
        WalletRequest request = new WalletRequest();
        request.setWalletId(testWalletId.toString());
        request.setOperationType(OperationType.DEPOSIT);
        request.setAmount(testAmount);

        WalletResponse response = new WalletResponse(testWalletId, testAmount);
        given(walletService.processTransaction(any(WalletRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "walletId": "%s",
                            "operationType": "DEPOSIT",
                            "amount": 2000
                        }
                        """.formatted(testWalletId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
                .andExpect(jsonPath("$.balance").value(2000));
    }

    @Test
    void processTransaction_InvalidWalletId_ReturnsBadRequest() throws Exception {
        String invalidRequestJson = """
    {
        "walletId": "invalid-uuid",
        "operationType": "DEPOSIT",
        "amount": 1000
    }
    """;

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.walletId").value("Неверный формат UUID"));
    }

    @Test
    void processTransaction_NegativeAmount_ReturnsBadRequest() throws Exception {
        WalletRequest request = new WalletRequest();
        request.setWalletId(testWalletId.toString());
        request.setOperationType(OperationType.DEPOSIT);
        request.setAmount(new BigDecimal("-100"));

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.amount").exists());
    }

    @Test
    void processTransaction_MissingOperationType_ReturnsBadRequest() throws Exception {
        String invalidRequestJson = """
        {
            "walletId": "%s",
            "amount": 1000
        }
        """.formatted(testWalletId);

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.operationType").exists());
    }

    @Test
    void processTransaction_WithdrawFromNonExistingWallet_ReturnsNotFound() throws Exception {
        WalletRequest request = new WalletRequest();
        request.setWalletId(testWalletId.toString());
        request.setOperationType(OperationType.DEPOSIT);
        request.setAmount(testAmount);

        when(walletService.processTransaction(any(WalletRequest.class)))
                .thenThrow(new WalletNotFoundException("Счёт не найден"));

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void processTransaction_invalidAmountFormat_returnsBadRequest() throws Exception {
        String invalidRequest = """
    {
        "walletId": "550e8400-e29b-41d4-a716-446655440000",
        "operationType": "DEPOSIT",
        "amount": "not-a-number"
    }
    """;

        mockMvc.perform(post("/api/v1/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный формат суммы. Укажите числовое значение"));
    }

    @Test
    void getBalance_existingWallet_returnsOkWithBalance() throws Exception {
        WalletResponse response = new WalletResponse(testWalletId, testAmount);
        when(walletService.getBalance(testWalletId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/wallets/{walletId}", testWalletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(testWalletId.toString()))
                .andExpect(jsonPath("$.balance").value(2000.00));
    }

    @Test
    void getBalance_nonExistingWallet_returnsNotFound() throws Exception {
        when(walletService.getBalance(testWalletId))
                .thenThrow(new WalletNotFoundException("Счёт не найден"));

        mockMvc.perform(get("/api/v1/wallets/{walletId}", testWalletId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void getBalance_invalidUuidFormat_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{walletId}", "invalid-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный формат UUID"));
    }

    @Test
    void getBalance_whitespaceUuid_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/%20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Неверный формат UUID"));
    }

}