package com.example.project.domain.payment.service.query;

import com.example.project.domain.payment.domain.DriverPayoutBatch;
import com.example.project.domain.payment.domain.DriverPayoutItem;
import com.example.project.domain.payment.domain.FeeInvoice;
import com.example.project.domain.payment.domain.FeeInvoiceItem;
import com.example.project.domain.payment.domain.TransportPayment;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.FeeInvoiceStatus;
import com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PayoutStatus;
import com.example.project.domain.payment.dto.paymentResponse.DriverPayoutItemStatusResponse;
import com.example.project.domain.payment.dto.paymentResponse.FeeInvoiceStatusResponse;
import com.example.project.domain.payment.repository.DriverPayoutBatchRepository;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.FeeAutoChargeAttemptRepository;
import com.example.project.domain.payment.repository.FeeInvoiceItemRepository;
import com.example.project.domain.payment.repository.FeeInvoiceRepository;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.repository.PaymentGatewayWebhookEventRepository;
import com.example.project.domain.payment.repository.ShipperBillingAgreementRepository;
import com.example.project.domain.payment.repository.TransportPaymentRepository;
import com.example.project.member.repository.DriverRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPaymentStatusQueryServiceTest {

    @Mock
    private PaymentDisputeRepository paymentDisputeRepository;

    @Mock
    private FeeInvoiceRepository feeInvoiceRepository;

    @Mock
    private FeeInvoiceItemRepository feeInvoiceItemRepository;

    @Mock
    private ShipperBillingAgreementRepository shipperBillingAgreementRepository;

    @Mock
    private FeeAutoChargeAttemptRepository feeAutoChargeAttemptRepository;

    @Mock
    private DriverPayoutBatchRepository driverPayoutBatchRepository;

    @Mock
    private DriverPayoutItemRepository driverPayoutItemRepository;

    @Mock
    private PaymentGatewayTransactionRepository paymentGatewayTransactionRepository;

    @Mock
    private PaymentGatewayWebhookEventRepository paymentGatewayWebhookEventRepository;

    @Mock
    private TransportPaymentRepository transportPaymentRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AdminPaymentStatusQueryService adminPaymentStatusQueryService;

    @Test
    void getFeeInvoiceStatus_usesLedgerSnapshotsForTotals() {
        FeeInvoice invoice = FeeInvoice.issue(10L, "2026-03", new BigDecimal("2000.00"), LocalDateTime.of(2026, 3, 31, 23, 59, 59));
        ReflectionTestUtils.setField(invoice, "invoiceId", 88L);
        ReflectionTestUtils.setField(invoice, "status", FeeInvoiceStatus.ISSUED);
        FeeInvoiceItem item = FeeInvoiceItem.builder()
                .itemId(1L)
                .invoiceId(88L)
                .orderId(501L)
                .feeAmount(new BigDecimal("2000.00"))
                .shipperChargeAmount(new BigDecimal("102000.00"))
                .shipperFeeRate(new BigDecimal("0.0200"))
                .driverFeeRate(new BigDecimal("0.0250"))
                .driverFeeAmount(new BigDecimal("2500.00"))
                .driverPayoutAmount(new BigDecimal("97500.00"))
                .tossFeeRate(new BigDecimal("0.0000"))
                .tossFeeAmount(new BigDecimal("0.00"))
                .platformGrossRevenue(new BigDecimal("4500.00"))
                .platformNetRevenue(new BigDecimal("4500.00"))
                .createdAt(LocalDateTime.of(2026, 3, 15, 12, 0))
                .build();
        TransportPayment payment = TransportPayment.ready(
                501L,
                10L,
                20L,
                new BigDecimal("99999.00"),
                new BigDecimal("0.0100"),
                new BigDecimal("1000.00"),
                new BigDecimal("98000.00"),
                com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod.TRANSFER
        );

        when(feeInvoiceRepository.findByShipperUserIdAndPeriod(10L, "2026-03")).thenReturn(Optional.of(invoice));
        when(feeInvoiceItemRepository.findAllByInvoiceId(88L)).thenReturn(List.of(item));
        when(transportPaymentRepository.findAllByOrderIdIn(List.of(501L))).thenReturn(List.of(payment));

        FeeInvoiceStatusResponse response = adminPaymentStatusQueryService.getFeeInvoiceStatus(10L, "2026-03");

        assertThat(response.itemCount()).isEqualTo(1L);
        assertThat(response.totalShipperChargeAmount()).isEqualByComparingTo("102000.00");
        assertThat(response.totalDriverPayoutAmount()).isEqualByComparingTo("97500.00");
        assertThat(response.totalDriverFeeAmount()).isEqualByComparingTo("2500.00");
        assertThat(response.totalPlatformGrossRevenue()).isEqualByComparingTo("4500.00");
        assertThat(response.items().get(0).amountSnapshot().shipperChargeAmount()).isEqualByComparingTo("102000.00");
        assertThat(response.items().get(0).amountSnapshot().shipperFeeRate()).isEqualByComparingTo("0.0200");
    }

    @Test
    void getPayoutItemStatusByOrderId_prefersLedgerItemSnapshotOverLivePayment() {
        DriverPayoutBatch batch = DriverPayoutBatch.builder()
                .batchId(44L)
                .batchDate(LocalDate.of(2026, 3, 16))
                .status(PayoutStatus.REQUESTED)
                .build();
        DriverPayoutItem item = DriverPayoutItem.builder()
                .itemId(9L)
                .batch(batch)
                .orderId(601L)
                .driverUserId(40L)
                .netAmount(new BigDecimal("97500.00"))
                .shipperChargeAmount(new BigDecimal("102000.00"))
                .shipperFeeRate(new BigDecimal("0.0200"))
                .shipperFeeAmount(new BigDecimal("2000.00"))
                .driverFeeRate(new BigDecimal("0.0250"))
                .driverFeeAmount(new BigDecimal("2500.00"))
                .tossFeeRate(new BigDecimal("0.0000"))
                .tossFeeAmount(new BigDecimal("0.00"))
                .platformGrossRevenue(new BigDecimal("4500.00"))
                .platformNetRevenue(new BigDecimal("4500.00"))
                .status(PayoutStatus.REQUESTED)
                .retryCount(0)
                .build();
        TransportPayment payment = TransportPayment.ready(
                601L,
                10L,
                40L,
                new BigDecimal("88888.00"),
                new BigDecimal("0.0100"),
                new BigDecimal("1000.00"),
                new BigDecimal("87000.00"),
                com.example.project.domain.payment.domain.paymentEnum.PaymentEnums.PaymentMethod.TRANSFER
        );

        when(driverPayoutItemRepository.findByOrderId(601L)).thenReturn(Optional.of(item));
        when(transportPaymentRepository.findByOrderId(601L)).thenReturn(Optional.of(payment));
        when(driverRepository.findByUser_UserId(40L)).thenReturn(Optional.empty());

        DriverPayoutItemStatusResponse response = adminPaymentStatusQueryService.getPayoutItemStatusByOrderId(601L);

        assertThat(response.amountSnapshot().shipperChargeAmount()).isEqualByComparingTo("102000.00");
        assertThat(response.amountSnapshot().driverFeeAmount()).isEqualByComparingTo("2500.00");
        assertThat(response.amountSnapshot().driverPayoutAmount()).isEqualByComparingTo("97500.00");
        assertThat(response.amountSnapshot().platformNetRevenue()).isEqualByComparingTo("4500.00");
    }
}
