package in.abhiikhula.queue.controller;

import in.abhiikhula.common.dto.ApiResponse;
import in.abhiikhula.common.exception.UnauthorizedException;
import in.abhiikhula.queue.dto.CancelTokenRequest;
import in.abhiikhula.queue.dto.JoinQueueRequest;
import in.abhiikhula.queue.dto.QueueStatusResponseDto;
import in.abhiikhula.queue.dto.TokenResponseDto;
import in.abhiikhula.queue.service.QueueService;
import in.abhiikhula.user.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "M3 Virtual Queue Engine", description = "Digital Token Issuance & Queue Progression Lifecycle (BRD Section 14, 29)")
public class QueueController {

    private final QueueService queueService;
    private final in.abhiikhula.queue.sse.QueueSseBroadcaster queueSseBroadcaster;
    private final in.abhiikhula.user.repository.UserRepository userRepository;

    @PostMapping({"/api/shops/{shopId}/queue/tokens/join", "/api/shops/{shopId}/tokens"})
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Join shop virtual queue", description = "Customer requests a digital queue token with estimated wait time and live people ahead counter.")
    public ResponseEntity<ApiResponse<TokenResponseDto>> joinQueue(
            @PathVariable UUID shopId,
            @Valid @RequestBody(required = false) JoinQueueRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            String phone = (request != null && request.getCustomerPhone() != null) ? request.getCustomerPhone().replaceAll("\\D", "") : null;
            if (phone != null && phone.length() >= 10) {
                if (phone.length() > 10) phone = phone.substring(phone.length() - 10);
                final String cleanPhone = phone;
                currentUser = userRepository.findByPhone(cleanPhone)
                        .orElseGet(() -> userRepository.save(User.builder()
                                .phone(cleanPhone)
                                .name(request.getCustomerName() != null && !request.getCustomerName().isBlank() ? request.getCustomerName().trim() : "Customer")
                                .role(in.abhiikhula.user.model.UserRole.CUSTOMER)
                                .language("hi")
                                .blocked(false)
                                .build()));
            } else {
                throw new UnauthorizedException("Login or 10-digit mobile number required to join queue");
            }
        }

        JoinQueueRequest req = request != null ? request : new JoinQueueRequest();
        TokenResponseDto token = queueService.joinQueue(shopId, req, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Successfully joined the queue"));
    }

    @GetMapping("/api/shops/{shopId}/queue/tokens")
    @Operation(summary = "Get all active queue tokens for shop", description = "Fetches all tokens for the active queue session of the shop")
    public ResponseEntity<ApiResponse<List<TokenResponseDto>>> getShopTokens(@PathVariable UUID shopId) {
        List<TokenResponseDto> tokens = queueService.getShopTokens(shopId);
        return ResponseEntity.ok(ApiResponse.ok(tokens, "Shop tokens fetched successfully"));
    }

    @PostMapping("/api/shops/{shopId}/queue/walk-in")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Merchant: Issue offline walk-in token", description = "Allows shopkeeper to add a walk-in / non-smartphone customer directly into the unified queue (BRD FR-704).")
    public ResponseEntity<ApiResponse<TokenResponseDto>> issueWalkInToken(
            @PathVariable UUID shopId,
            @Valid @RequestBody(required = false) in.abhiikhula.queue.dto.IssueWalkInTokenRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        in.abhiikhula.queue.dto.IssueWalkInTokenRequest req = request != null ? request : new in.abhiikhula.queue.dto.IssueWalkInTokenRequest();
        TokenResponseDto token = queueService.issueWalkInToken(shopId, req, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Walk-in token issued successfully"));
    }

    @PostMapping("/api/shops/{shopId}/queue/next")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Merchant: Call next customer in queue", description = "Advances the queue session by calling the next waiting customer (WAITING -> CALLED). Only authorized shop owner.")
    public ResponseEntity<ApiResponse<TokenResponseDto>> callNext(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        TokenResponseDto token = queueService.callNext(shopId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Next customer called successfully"));
    }

    @PostMapping("/api/shops/{shopId}/queue/tokens/{tokenId}/complete")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Merchant: Complete customer service", description = "Marks the token service as COMPLETED. Only authorized shop owner.")
    public ResponseEntity<ApiResponse<TokenResponseDto>> completeToken(
            @PathVariable UUID shopId,
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        TokenResponseDto token = queueService.completeToken(shopId, tokenId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Token service marked completed"));
    }

    @PostMapping("/api/shops/{shopId}/queue/tokens/{tokenId}/no-show")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Merchant: Mark customer as NO_SHOW", description = "Marks customer as NO_SHOW if absent when called. Only authorized shop owner.")
    public ResponseEntity<ApiResponse<TokenResponseDto>> markNoShow(
            @PathVariable UUID shopId,
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        TokenResponseDto token = queueService.markNoShow(shopId, tokenId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Token marked as NO_SHOW"));
    }

    @PostMapping("/api/shops/{shopId}/queue/tokens/{tokenId}/skip")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Merchant: Skip / Push back waiting customer", description = "Marks customer as SKIPPED if they are late or stepping away. Only authorized shop owner.")
    public ResponseEntity<ApiResponse<TokenResponseDto>> skipToken(
            @PathVariable UUID shopId,
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        TokenResponseDto token = queueService.skipToken(shopId, tokenId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Token marked as SKIPPED"));
    }

    @PostMapping("/api/shops/{shopId}/queue/delay")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Merchant: Announce / Update queue delay", description = "Updates extra delay minutes and reason for this session. Broadcasts to waiting customers.")
    public ResponseEntity<ApiResponse<QueueStatusResponseDto>> updateQueueDelay(
            @PathVariable UUID shopId,
            @Valid @RequestBody in.abhiikhula.queue.dto.UpdateQueueDelayRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        QueueStatusResponseDto status = queueService.updateQueueDelay(shopId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(status, "Queue delay updated successfully"));
    }

    @PostMapping("/api/queue/tokens/{tokenId}/cancel")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Cancel queue token", description = "Allows customer (or merchant) to cancel a waiting token with optional reason.")
    public ResponseEntity<ApiResponse<TokenResponseDto>> cancelToken(
            @PathVariable UUID tokenId,
            @RequestBody(required = false) CancelTokenRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        TokenResponseDto token = queueService.cancelToken(tokenId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Token cancelled successfully"));
    }

    @PostMapping("/api/tokens/{tokenId}/checkin")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Customer Geo Check-in", description = "Verifies customer is within 100m of the shop and marks token as checked-in (BRD FR-307).")
    public ResponseEntity<ApiResponse<TokenResponseDto>> checkIn(
            @PathVariable UUID tokenId,
            @Valid @RequestBody in.abhiikhula.queue.dto.CheckInRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        TokenResponseDto token = queueService.checkIn(tokenId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Checked in successfully at the shop"));
    }

    @PostMapping("/api/tokens/{tokenId}/requeue")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Customer Re-queue after Skip/No-show", description = "Allows a skipped or no-show customer to re-enter at the back of the queue once (BRD FR-309).")
    public ResponseEntity<ApiResponse<TokenResponseDto>> reQueue(
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        TokenResponseDto token = queueService.reQueue(tokenId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Token re-queued successfully at the back"));
    }

    @PostMapping("/api/shops/{shopId}/queue/pause")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Merchant: Pause queue session", description = "Temporarily pauses new token issuance for 15, 30, or 60 minutes (BRD FR-707).")
    public ResponseEntity<ApiResponse<QueueStatusResponseDto>> pauseQueue(
            @PathVariable UUID shopId,
            @Valid @RequestBody in.abhiikhula.queue.dto.PauseQueueRequest request,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        QueueStatusResponseDto status = queueService.pauseQueue(shopId, request, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(status, "Queue paused successfully"));
    }

    @PostMapping("/api/shops/{shopId}/queue/resume")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Merchant: Resume paused queue", description = "Resumes a paused queue session back to ACTIVE (BRD FR-707).")
    public ResponseEntity<ApiResponse<QueueStatusResponseDto>> resumeQueue(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        QueueStatusResponseDto status = queueService.resumeQueue(shopId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(status, "Queue resumed successfully"));
    }

    @PostMapping("/api/shops/{shopId}/queue/reset")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Merchant: Reset queue session", description = "Closes current session, expires pending tokens, and starts fresh sequence from #1 (BRD FR-708, FR-314).")
    public ResponseEntity<ApiResponse<QueueStatusResponseDto>> resetQueue(
            @PathVariable UUID shopId,
            @AuthenticationPrincipal User currentUser) {

        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        QueueStatusResponseDto status = queueService.resetQueue(shopId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(status, "Queue reset successfully. New session started."));
    }

    @GetMapping("/api/shops/{shopId}/queue/status")
    @Operation(summary = "Get live shop queue status & wait time", description = "Public endpoint for customers to view current serving token and total waiting count.")
    public ResponseEntity<ApiResponse<QueueStatusResponseDto>> getQueueStatus(@PathVariable UUID shopId) {
        QueueStatusResponseDto status = queueService.getQueueStatus(shopId);
        return ResponseEntity.ok(ApiResponse.ok(status, "Live queue status fetched successfully"));
    }

    @GetMapping(value = "/api/shops/{shopId}/queue/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live real-time queue updates (SSE)", description = "Streams live queue changes, current serving token, and waiting counts in real time via Server-Sent Events (BRD Section 22).")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamQueueStatus(@PathVariable UUID shopId) {
        return queueSseBroadcaster.subscribe(shopId, queueService.getQueueStatus(shopId));
    }

    @GetMapping("/api/queue/tokens/my")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Customer: Get my active queue tokens", description = "Fetches all active waiting/called tokens for the logged-in customer.")
    public ResponseEntity<ApiResponse<List<TokenResponseDto>>> getMyActiveTokens(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException("Authentication required");
        }

        List<TokenResponseDto> tokens = queueService.getMyActiveTokens(currentUser);
        return ResponseEntity.ok(ApiResponse.ok(tokens, "Active tokens fetched successfully"));
    }

    @GetMapping("/api/tokens/{tokenId}")
    @Operation(summary = "Get queue token status & live ETA", description = "Fetches individual token progress, people ahead count, and estimated wait (BRD Section 30, FR-1603). Masks phone for privacy.")
    public ResponseEntity<ApiResponse<TokenResponseDto>> getTokenById(
            @PathVariable UUID tokenId,
            @AuthenticationPrincipal User currentUser) {
        TokenResponseDto token = queueService.getTokenById(tokenId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(token, "Token status fetched successfully"));
    }

    @GetMapping("/api/tokens/group/{groupId}")
    @Operation(summary = "Get all companion tokens in a group booking", description = "Fetches all sequential tokens booked together for family/friends in a single party.")
    public ResponseEntity<ApiResponse<List<TokenResponseDto>>> getGroupTokens(
            @PathVariable UUID groupId,
            @AuthenticationPrincipal User currentUser) {
        List<TokenResponseDto> tokens = queueService.getGroupTokens(groupId, currentUser);
        return ResponseEntity.ok(ApiResponse.ok(tokens, "Group tokens fetched successfully"));
    }
}

