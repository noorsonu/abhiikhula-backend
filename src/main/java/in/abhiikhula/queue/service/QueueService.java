package in.abhiikhula.queue.service;

import in.abhiikhula.queue.dto.CancelTokenRequest;
import in.abhiikhula.queue.dto.JoinQueueRequest;
import in.abhiikhula.queue.dto.QueueStatusResponseDto;
import in.abhiikhula.queue.dto.TokenResponseDto;
import in.abhiikhula.user.model.User;

import java.util.List;
import java.util.UUID;

public interface QueueService {

    TokenResponseDto joinQueue(UUID shopId, JoinQueueRequest request, User currentUser);

    TokenResponseDto issueWalkInToken(UUID shopId, in.abhiikhula.queue.dto.IssueWalkInTokenRequest request, User currentUser);

    TokenResponseDto callNext(UUID shopId, User currentUser);

    TokenResponseDto completeToken(UUID shopId, UUID tokenId, User currentUser);

    TokenResponseDto markNoShow(UUID shopId, UUID tokenId, User currentUser);

    TokenResponseDto skipToken(UUID shopId, UUID tokenId, User currentUser);

    QueueStatusResponseDto updateQueueDelay(UUID shopId, in.abhiikhula.queue.dto.UpdateQueueDelayRequest request, User currentUser);

    TokenResponseDto cancelToken(UUID tokenId, CancelTokenRequest request, User currentUser);

    TokenResponseDto checkIn(UUID tokenId, in.abhiikhula.queue.dto.CheckInRequest request, User currentUser);

    TokenResponseDto reQueue(UUID tokenId, User currentUser);

    TokenResponseDto getTokenById(UUID tokenId, User currentUser);

    QueueStatusResponseDto pauseQueue(UUID shopId, in.abhiikhula.queue.dto.PauseQueueRequest request, User currentUser);

    QueueStatusResponseDto resumeQueue(UUID shopId, User currentUser);

    QueueStatusResponseDto resetQueue(UUID shopId, User currentUser);

    QueueStatusResponseDto getQueueStatus(UUID shopId);

    List<TokenResponseDto> getMyActiveTokens(User currentUser);

    List<TokenResponseDto> getGroupTokens(UUID groupId, User currentUser);
}

