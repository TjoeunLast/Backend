package com.example.project.domain.payment.controller;

import com.example.project.domain.payment.dto.paymentRequest.FeePreviewRequest;
import com.example.project.domain.payment.dto.paymentResponse.FeeBreakdownPreviewResponse;
import com.example.project.domain.payment.repository.DriverPayoutItemRepository;
import com.example.project.domain.payment.repository.FeeInvoiceRepository;
import com.example.project.domain.payment.repository.PaymentDisputeRepository;
import com.example.project.domain.payment.repository.PaymentGatewayTransactionRepository;
import com.example.project.domain.payment.service.core.FeeInvoiceService;
import com.example.project.domain.payment.service.core.ShipperBillingAgreementService;
import com.example.project.domain.payment.service.core.TransportPaymentService;
import com.example.project.domain.payment.service.query.AdminPaymentStatusQueryService;
import com.example.project.domain.order.repository.OrderRepository;
import com.example.project.global.api.ApiResponse;
import com.example.project.member.domain.Users;
import com.example.project.member.repository.UsersRepository;
import com.example.project.security.user.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerFeePreviewTest {

    @Mock
    private TransportPaymentService transportPaymentService;

    @Mock
    private ShipperBillingAgreementService shipperBillingAgreementService;

    @Mock
    private FeeInvoiceService feeInvoiceService;

    @Mock
    private AdminPaymentStatusQueryService adminPaymentStatusQueryService;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentDisputeRepository paymentDisputeRepository;

    @Mock
    private FeeInvoiceRepository feeInvoiceRepository;

    @Mock
    private DriverPayoutItemRepository driverPayoutItemRepository;

    @Mock
    private PaymentGatewayTransactionRepository paymentGatewayTransactionRepository;

    @Mock
    private UsersRepository usersRepository;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void previewFeeMethodRequiresShipperOrAdminRole() throws Exception {
        Method method = PaymentController.class.getMethod(
                "previewFee",
                FeePreviewRequest.class,
                Users.class
        );

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasAnyRole('SHIPPER','ADMIN')");
    }

    @Test
    void previewFeeReturnsWrappedServicePayload() {
        Users shipper = Users.builder()
                .userId(10L)
                .role(Role.SHIPPER)
                .email("shipper@test.com")
                .build();
        FeePreviewRequest request = new FeePreviewRequest();
        ReflectionTestUtils.setField(request, "baseAmount", new BigDecimal("100000"));

        FeeBreakdownPreviewResponse previewResponse = FeeBreakdownPreviewResponse.builder()
                .previewMode("ORDER_CREATE")
                .baseAmount(new BigDecimal("100000.00"))
                .shipperChargeAmount(new BigDecimal("103000.00"))
                .build();

        when(transportPaymentService.previewFee(same(shipper), same(request))).thenReturn(previewResponse);

        ApiResponse<FeeBreakdownPreviewResponse> response = paymentController.previewFee(request, shipper);

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isNull();
        assertThat(response.data()).isSameAs(previewResponse);
    }
}
