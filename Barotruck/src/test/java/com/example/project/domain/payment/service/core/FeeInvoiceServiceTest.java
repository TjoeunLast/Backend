package com.example.project.domain.payment.service.core;

import com.example.project.domain.payment.domain.FeeInvoice;
import com.example.project.domain.payment.domain.FeeInvoiceItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.TransportPaymentPricingSnapshot;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod;
import com.example.project.domain.payment.repository.FeeInvoiceItemRepository;
import com.example.project.domain.payment.repository.FeeInvoiceRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeInvoiceServiceTest {

    @Mock
    private FeeInvoiceRepository feeInvoiceRepository;

    @Mock
    private FeeInvoiceItemRepository feeInvoiceItemRepository;

    @Mock
    private TransportPaymentRepository transportPaymentRepository;

    @InjectMocks
    private FeeInvoiceService feeInvoiceService;

    @Test
    void generateForShipper_copiesLedgerSnapshotIntoInvoiceItems() {
        TransportPayment payment = transferPayment(51L);

        when(feeInvoiceRepository.findByShipperUserIdAndPeriod(20L, "2026-03")).thenReturn(Optional.empty());
        when(transportPaymentRepository.findAll()).thenReturn(List.of(payment));
        when(feeInvoiceRepository.save(any(FeeInvoice.class))).thenAnswer(invocation -> {
            FeeInvoice invoice = invocation.getArgument(0);
            if (invoice.getInvoiceId() == null) {
                ReflectionTestUtils.setField(invoice, "invoiceId", 300L);
            }
            return invoice;
        });
        when(feeInvoiceItemRepository.existsByInvoiceIdAndOrderId(300L, 51L)).thenReturn(false);
        when(feeInvoiceItemRepository.save(any(FeeInvoiceItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeeInvoice invoice = feeInvoiceService.generateForShipper(20L, YearMonth.of(2026, 3));

        ArgumentCaptor<FeeInvoiceItem> itemCaptor = ArgumentCaptor.forClass(FeeInvoiceItem.class);
        verify(feeInvoiceItemRepository).save(itemCaptor.capture());
        FeeInvoiceItem savedItem = itemCaptor.getValue();

        assertThat(invoice.getTotalFee()).isEqualByComparingTo("2000.00");
        assertThat(savedItem.getOrderId()).isEqualTo(51L);
        assertThat(savedItem.getShipperChargeAmount()).isEqualByComparingTo("102000.00");
        assertThat(savedItem.getShipperFeeAmount()).isEqualByComparingTo("2000.00");
        assertThat(savedItem.getDriverFeeAmount()).isEqualByComparingTo("2500.00");
        assertThat(savedItem.getDriverPayoutAmount()).isEqualByComparingTo("97500.00");
        assertThat(savedItem.getPlatformGrossRevenue()).isEqualByComparingTo("4500.00");
        assertThat(savedItem.getPlatformNetRevenue()).isEqualByComparingTo("4500.00");
    }

    private TransportPayment transferPayment(Long orderId) {
        TransportPayment payment = TransportPayment.ready(
                orderId,
                20L,
                40L,
                new BigDecimal("102000.00"),
                new BigDecimal("0.0200"),
                new BigDecimal("2000.00"),
                new BigDecimal("97500.00"),
                PaymentMethod.TRANSFER
        );
        payment.applyPricingSnapshot(new TransportPaymentPricingSnapshot(
                new BigDecimal("102000.00"),
                new BigDecimal("100000.00"),
                new BigDecimal("0.0200"),
                new BigDecimal("2000.00"),
                false,
                new BigDecimal("102000.00"),
                new BigDecimal("0.0250"),
                new BigDecimal("2500.00"),
                false,
                new BigDecimal("97500.00"),
                new BigDecimal("0.0000"),
                new BigDecimal("0.00"),
                new BigDecimal("4500.00"),
                new BigDecimal("4500.00"),
                1L,
                LocalDateTime.of(2026, 3, 1, 0, 0)
        ));
        payment.markPaid("BANK-1", LocalDateTime.of(2026, 3, 15, 12, 0));
        return payment;
    }
}
