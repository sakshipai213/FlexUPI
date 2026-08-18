package com.fintech.upisimulator.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import com.fintech.upisimulator.dto.PaymentRequest;
import com.fintech.upisimulator.dto.PaymentResponse;
import com.fintech.upisimulator.exception.InsufficientCreditException;
import com.fintech.upisimulator.model.UserAccount;
import com.fintech.upisimulator.repository.LedgerEntryRepository;
import com.fintech.upisimulator.repository.TransactionRepository;
import com.fintech.upisimulator.repository.UserAccountRepository;
import com.fintech.upisimulator.strategy.FeeCalculationStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class CreditPaymentServiceTest {

    @Mock private UserAccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private LedgerEntryRepository ledgerRepository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private FeeCalculationStrategy feeStrategy;

    @InjectMocks
    private CreditPaymentService creditPaymentService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testProcessPayment_Success() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        UserAccount user = new UserAccount();
        user.setVpa("john@upi");
        user.setAvailableCredit(new BigDecimal("50000"));
        user.setCreditLimit(new BigDecimal("50000"));

        when(accountRepository.findByVpa("john@upi")).thenReturn(Optional.of(user));
        when(feeStrategy.calculateFee(any())).thenReturn(BigDecimal.ZERO);

        PaymentRequest request = new PaymentRequest("john@upi", "merchant@upi", new BigDecimal("5000"));
        PaymentResponse response = creditPaymentService.processPayment("tx-999", request);

        assertNotNull(response);
        assertEquals("SETTLED", response.status());
        verify(accountRepository, times(1)).save(any());
        verify(ledgerRepository, times(2)).save(any());
    }

    @Test
    void testProcessPayment_InsufficientCredit_ThrowsException() {
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(true);

        UserAccount user = new UserAccount();
        user.setVpa("john@upi");
        user.setAvailableCredit(new BigDecimal("1000"));

        when(accountRepository.findByVpa("john@upi")).thenReturn(Optional.of(user));
        when(feeStrategy.calculateFee(any())).thenReturn(BigDecimal.ZERO);

        PaymentRequest request = new PaymentRequest("john@upi", "merchant@upi", new BigDecimal("5000"));

        assertThrows(InsufficientCreditException.class, () -> 
            creditPaymentService.processPayment("tx-999", request)
        );
    }
}