package in.abhiikhula.queue.mapper;

import in.abhiikhula.queue.dto.QueueStatusResponseDto;
import in.abhiikhula.queue.dto.TokenResponseDto;
import in.abhiikhula.queue.model.QueueSession;
import in.abhiikhula.queue.model.Token;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QueueMapper {

    @Mapping(target = "shopId", source = "token.shop.id")
    @Mapping(target = "shopName", source = "token.shop.name")
    @Mapping(target = "sessionId", source = "token.session.id")
    @Mapping(target = "delayMinutes", source = "token.session.delayMinutes")
    @Mapping(target = "delayReason", source = "token.session.delayReason")
    @Mapping(target = "peopleAhead", source = "peopleAhead")
    TokenResponseDto toTokenResponseDto(Token token, Integer peopleAhead);

    @Mapping(target = "shopId", source = "token.shop.id")
    @Mapping(target = "shopName", source = "token.shop.name")
    @Mapping(target = "sessionId", source = "token.session.id")
    @Mapping(target = "delayMinutes", source = "token.session.delayMinutes")
    @Mapping(target = "delayReason", source = "token.session.delayReason")
    @Mapping(target = "peopleAhead", constant = "0")
    TokenResponseDto toTokenResponseDto(Token token);

    List<TokenResponseDto> toTokenResponseDtoList(List<Token> tokens);

    @Mapping(target = "shopId", source = "session.shop.id")
    @Mapping(target = "shopName", source = "session.shop.name")
    @Mapping(target = "sessionId", source = "session.id")
    @Mapping(target = "sessionStatus", source = "session.status")
    @Mapping(target = "currentServingTokenNumber", source = "session.currentTokenNumber")
    @Mapping(target = "currentServingDisplayToken", source = "servingDisplayToken")
    @Mapping(target = "totalTokensIssued", source = "session.totalTokensIssued")
    @Mapping(target = "waitingCount", source = "waitingCount")
    @Mapping(target = "estimatedWaitMin", source = "estimatedWaitMin")
    @Mapping(target = "delayMinutes", source = "session.delayMinutes")
    @Mapping(target = "delayReason", source = "session.delayReason")
    @Mapping(target = "pausedUntil", source = "session.pausedUntil")
    @Mapping(target = "pauseReason", source = "session.pauseReason")
    @Mapping(target = "acceptingTokens", source = "acceptingTokens")
    QueueStatusResponseDto toQueueStatusResponseDto(
            QueueSession session,
            Integer waitingCount,
            Integer estimatedWaitMin,
            boolean acceptingTokens,
            String servingDisplayToken);
}

