package com.gideonheritagebank.IDENTITY_SERVICE.service;

import com.gideonheritagebank.IDENTITY_SERVICE.dto.request.CreateCustomerClosureRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.dto.response.CustomerClosureResponse;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.BankUser;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.CustomerClosureRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.EmailVerificationToken;
import com.gideonheritagebank.IDENTITY_SERVICE.entity.PasswordResetToken;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerClosureStatus;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.UserType;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BadRequest;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.BusinessException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ConflictException;
import com.gideonheritagebank.IDENTITY_SERVICE.exception.ResourceNotFoundException;
import com.gideonheritagebank.IDENTITY_SERVICE.mapper.IdentityMapper;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.BankUserRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.CustomerClosureRequestRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.EmailVerificationTokenRepository;
import com.gideonheritagebank.IDENTITY_SERVICE.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerClosureService {

    @Value("${identity.retention.closed-customer-review-period:P2555D}")
    private Duration closedCustomerReviewPeriod;

    private static final List<CustomerClosureStatus> OPEN_STATUSES = List.of(
            CustomerClosureStatus.REQUESTED,
            CustomerClosureStatus.UNDER_REVIEW,
            CustomerClosureStatus.AWAITING_FINANCIAL_CLEARANCE,
            CustomerClosureStatus.READY_TO_CLOSE
    );

    private final BankUserRepository bankUserRepository;
    private final CustomerClosureRequestRepository closureRequestRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;
    private final IdentityMapper identityMapper;

    /* CUSTOMER SELF-SERVICE */
    @Transactional
    public CustomerClosureResponse requestClosure(
            UUID customerId,
            CreateCustomerClosureRequest request
    ) {
        BankUser customer = getCustomerForUpdate(customerId);

        if (customer.isIdentityClosed()) {
            throw new ConflictException("This customer identity is already closed.");
        }

        if (!passwordEncoder.matches(request.currentPassword(), customer.getPasswordHash())) {
            throw new BadRequest("The supplied password is incorrect.");
        }

        boolean hasOpenRequest = closureRequestRepository.existsByCustomer_IdAndStatusIn(
                customerId,
                OPEN_STATUSES
        );

        if (hasOpenRequest) {
            throw new ConflictException("An active closure request already exists.");
        }

        String reason = normalizeReason(request.reason());

        CustomerClosureRequest closureRequest =
                new CustomerClosureRequest(customer, reason);

        CustomerClosureRequest saved =
                closureRequestRepository.saveAndFlush(closureRequest);

        return identityMapper.toCustomerClosureResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerClosureResponse> getMyClosureRequests(
            UUID customerId
    ) {
        BankUser customer = getCustomer(customerId);

        return closureRequestRepository
                .findAllByCustomer_IdOrderByRequestedAtDesc(customer.getId())
                .stream()
                .map(identityMapper::toCustomerClosureResponse)
                .toList();
    }

    @Transactional
    public CustomerClosureResponse cancelMyClosureRequest(
            UUID customerId,
            UUID requestId
    ) {
        // Lock the customer first. All workflow operations use this
        // customer-then-request lock order to reduce deadlock risk.
        getCustomerForUpdate(customerId);

        CustomerClosureRequest closureRequest =
                closureRequestRepository.findByIdForUpdate(requestId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Closure request was not found."
                        ));

        if (!closureRequest.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException("Closure request was not found.");
        }

        if (closureRequest.getStatus() != CustomerClosureStatus.REQUESTED) {
            throw new ConflictException(
                    "A closure request can only be cancelled before staff review begins."
            );
        }

        closureRequest.cancel();

        return identityMapper.toCustomerClosureResponse(
                closureRequestRepository.saveAndFlush(closureRequest)
        );
    }

    /* ADMINISTRATIVE RETRIEVAL */
    @Transactional(readOnly = true)
    public Page<CustomerClosureResponse> getClosureRequests(
            CustomerClosureStatus status,
            Pageable pageable
    ) {
        Page<CustomerClosureRequest> requests;
        int pageNumber = Math.max(pageable.getPageNumber() - 1, 0);
        pageable = Pageable.ofSize(pageable.getPageSize()).withPage(pageNumber);

        if (status == null) {
            requests = closureRequestRepository.findAll(pageable);
        } else {
            requests = closureRequestRepository.findAllByStatusIn(
                    List.of(status),
                    pageable
            );
        }

        return requests.map(identityMapper::toCustomerClosureResponse);
    }

    @Transactional(readOnly = true)
    public CustomerClosureResponse getClosureRequest(UUID requestId) {

        CustomerClosureRequest request =
                closureRequestRepository.findById(requestId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Closure request was not found."
                        ));

        return identityMapper.toCustomerClosureResponse(request);
    }

    /* STAFF REVIEW */
    @Transactional
    public CustomerClosureResponse beginReview(
            UUID requestId,
            UUID staffId,
            String notes
    ) {
        CustomerClosureRequest closureRequest = getClosureRequestForUpdate(requestId);

        BankUser staff = getStaff(staffId);

        closureRequest.beginReview(
                staff,
                normalizeOptionalNotes(notes)
        );

        return identityMapper.toCustomerClosureResponse(
                closureRequestRepository.saveAndFlush(closureRequest)
        );
    }

    /* FINANCIAL CLEARANCE */
    @Transactional
    public CustomerClosureResponse requestFinancialClearance(
            UUID requestId,
            UUID staffId,
            String notes
    ) {
        CustomerClosureRequest closureRequest = getClosureRequestForUpdate(requestId);

        getStaff(staffId);

        closureRequest.requestFinancialClearance(
                normalizeOptionalNotes(notes)
        );

        return identityMapper.toCustomerClosureResponse(
                closureRequestRepository.saveAndFlush(closureRequest)
        );
    }

    /* FINANCIAL CLEARANCE */
    @Transactional
    public CustomerClosureResponse confirmFinancialClearance(
            UUID requestId,
            UUID confirmerId,
            String reference,
            String notes
    ) {
        CustomerClosureRequest closureRequest = getClosureRequestForUpdate(requestId);

        BankUser confirmer = getStaff(confirmerId);

        String normalizedReference = normalizeRequiredReference(reference);

        closureRequest.confirmFinancialClearance(
                confirmer,
                normalizedReference,
                normalizeOptionalNotes(notes)
        );

        return identityMapper.toCustomerClosureResponse(
                closureRequestRepository.saveAndFlush(closureRequest)
        );
    }

    /* REJECTION */
    @Transactional
    public CustomerClosureResponse rejectClosureRequest(
            UUID requestId,
            UUID staffId,
            String reason
    ) {
        CustomerClosureRequest closureRequest = getClosureRequestForUpdate(requestId);

        BankUser staff = getStaff(staffId);

        closureRequest.reject(
                staff,
                normalizeReason(reason)
        );

        return identityMapper.toCustomerClosureResponse(
                closureRequestRepository.saveAndFlush(closureRequest)
        );
    }

    /* FINAL IDENTITY CLOSURE */
    @Transactional
    public CustomerClosureResponse completeClosure(
            UUID requestId,
            UUID staffId
    ) {
        /*
         * Read once to discover the customer ID. Then lock the customer
         * before locking the closure request. This preserves a consistent
         * lock order across the workflow.
         */
        CustomerClosureRequest initialRequest =
                closureRequestRepository.findById(requestId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Closure request was not found."
                        ));

        UUID customerId = initialRequest.getCustomer().getId();

        BankUser customer = getCustomerForUpdate(customerId);

        CustomerClosureRequest closureRequest =
                closureRequestRepository.findByIdForUpdate(requestId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Closure request was not found."
                        ));

        if (!closureRequest.getCustomer().getId().equals(customerId)) {
            throw new ResourceNotFoundException(
                    "Closure request was not found."
            );
        }

        BankUser staff = getStaff(staffId);

        if (closureRequest.getStatus() != CustomerClosureStatus.READY_TO_CLOSE) {
            throw new ConflictException(
                    "Financial clearance must be confirmed before closure."
            );
        }

        if (customer.isIdentityClosed()) {
            throw new ConflictException(
                    "Customer identity is already closed."
            );
        }

        Instant now = Instant.now();

        /* Close the identity. Do not delete the BankUser. */
        if (closedCustomerReviewPeriod == null
                || closedCustomerReviewPeriod.isNegative()
                || closedCustomerReviewPeriod.isZero()) {

            throw new BusinessException(
                    "The closed-customer retention review period is not configured correctly."
            );
        }

        customer.closeIdentity(
                closureRequest.getCustomerReason(),
                now.plus(closedCustomerReviewPeriod)
        );

        /* Revoke every active refresh-token session. */
        authenticationService.revokeAllActiveRefreshTokens(customer);

        /* Invalidate all outstanding email-verification tokens. */
        List<EmailVerificationToken> verificationTokens =
                emailVerificationTokenRepository.findAllByUser_Id(customerId);

        for (EmailVerificationToken token : verificationTokens) {
            if (token.getUsedAt() == null) {
                token.setUsedAt(now);
            }
        }

        if (!verificationTokens.isEmpty()) {
            emailVerificationTokenRepository.saveAll(verificationTokens);
        }

        /*
         * Invalidate all outstanding password-reset tokens.
         */
        List<PasswordResetToken> resetTokens =
                passwordResetTokenRepository.findAllByUser_Id(customerId);

        for (PasswordResetToken token : resetTokens) {
            if (token.getUsedAt() == null) {
                token.setUsedAt(now);
            }
        }

        if (!resetTokens.isEmpty()) {
            passwordResetTokenRepository.saveAll(resetTokens);
        }

        bankUserRepository.saveAndFlush(customer);

        closureRequest.complete(staff);

        closureRequestRepository.saveAndFlush(closureRequest);

        return identityMapper.toCustomerClosureResponse(closureRequest);
    }

    /* LOOKUP AND VALIDATION*/
    private BankUser getCustomer(UUID customerId) {

        BankUser customer = bankUserRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer identity was not found."
                ));

        validateCustomer(customer);

        return customer;
    }

    private BankUser getCustomerForUpdate(UUID customerId) {

        BankUser customer = bankUserRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer identity was not found."
                ));

        validateCustomer(customer);

        return customer;
    }

    private void validateCustomer(BankUser customer) {

        if (customer.getUserType() != UserType.CUSTOMER) {
            throw new ResourceNotFoundException("Customer identity was not found.");
        }
    }

    private BankUser getStaff(UUID staffId) {

        BankUser staff = bankUserRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Staff identity was not found."
                ));

        if (staff.getUserType() != UserType.STAFF
                || !staff.isEnabled()
                || !staff.isEmailVerified()
                || !staff.isAccountNonLocked()
                || staff.isSuspended()
                || staff.isIdentityClosed()) {

            throw new BusinessException("The acting identity is not an active staff identity.");
        }

        return staff;
    }

    private CustomerClosureRequest getClosureRequestForUpdate(
            UUID requestId
    ) {
        CustomerClosureRequest initial =
                closureRequestRepository.findById(requestId)
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Closure request was not found."
                        ));

        getCustomerForUpdate(initial.getCustomer().getId());

        return closureRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Closure request was not found."
                ));
    }

    private String normalizeReason(String reason) {

        if (reason == null || reason.isBlank()) {
            throw new BadRequest("A reason is required.");
        }

        String normalized = reason.trim();

        if (normalized.length() < 10 || normalized.length() > 1000) {
            throw new BadRequest("Reason must contain between 10 and 1000 characters.");
        }

        return normalized;
    }

    private String normalizeOptionalNotes(String notes) {

        if (notes == null || notes.isBlank()) {
            return null;
        }

        String normalized = notes.trim();

        if (normalized.length() > 1000) {
            throw new BadRequest("Notes cannot exceed 1000 characters.");
        }

        return normalized;
    }

    private String normalizeRequiredReference(String reference) {

        if (reference == null || reference.isBlank()) {
            throw new BadRequest("A financial clearance reference is required.");
        }

        String normalized = reference.trim();

        if (normalized.length() > 150) {
            throw new BadRequest("Financial clearance reference cannot exceed 150 characters.");
        }

        return normalized;
    }
}