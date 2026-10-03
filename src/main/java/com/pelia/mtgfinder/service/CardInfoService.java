package com.pelia.mtgfinder.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pelia.mtgfinder.dto.CardSearchResponse;
import com.pelia.mtgfinder.model.Card;
import com.pelia.mtgfinder.model.CardFace;
import com.pelia.mtgfinder.model.CardOracle;
import com.pelia.mtgfinder.repository.CardFaceRepository;
import com.pelia.mtgfinder.repository.CardOracleRepository;
import com.pelia.mtgfinder.repository.CardRepository;

@Service
@Transactional(readOnly = true)
public class CardInfoService {

    private final CardOracleRepository cardOracleRepository;
    private final CardRepository cardRepository;
    private final CardFaceRepository cardFaceRepository;
    private final CardResponseMapper responseMapper;

    public CardInfoService(
            CardOracleRepository cardOracleRepository,
            CardRepository cardRepository,
            CardFaceRepository cardFaceRepository,
            CardResponseMapper responseMapper) {
        this.cardOracleRepository = cardOracleRepository;
        this.cardRepository = cardRepository;
        this.cardFaceRepository = cardFaceRepository;
        this.responseMapper = responseMapper;
    }

    public Optional<CardSearchResponse.OracleCard> findByOracleId(UUID oracleId) {
        Objects.requireNonNull(oracleId, "Oracle ID must not be null");
        Optional<CardOracle> matchingOracle = cardOracleRepository.findForInfo(oracleId);
        if (matchingOracle.isEmpty()) {
            return Optional.empty();
        }

        CardOracle oracle = matchingOracle.get();
        List<Card> prints = cardRepository.findByCardIdentityId(oracle.getId());
        Map<UUID, List<CardFace>> facesByPrint = prints.isEmpty()
                ? Map.of()
                : cardFaceRepository.findByCardPrintIds(prints.stream().map(Card::getId).toList()).stream()
                        .collect(Collectors.groupingBy(CardFace::getCardPrintId));
        List<CardSearchResponse.CardPrint> printResponses = prints.stream()
                .map(print -> responseMapper.toCardPrint(print, facesByPrint.getOrDefault(print.getId(), List.of())))
                .toList();
        return Optional.of(responseMapper.toOracleCard(oracle, null, printResponses));
    }
}
