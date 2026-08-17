package com.elixircodex.backend.stack;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StackService {

    private final SupplementLogRepository supplementLogRepository;
    private final IngredientCardRepository ingredientCardRepository;

    public StackEvaluation evaluate(Long ownerId, StackRequest request) {
        List<SupplementLog> todayVerifiedLogs = supplementLogRepository
                .findByOwnerIdAndConsumedDateAndIsVerifiedTrue(ownerId, LocalDate.now());
        if (todayVerifiedLogs.isEmpty()) {
            throw new StackValidationException("오늘 인증된 영양제가 없습니다");
        }

        List<IngredientCard> cards = ingredientCardRepository.findAllById(request.ingredientCardIds());
        if (cards.isEmpty()) {
            throw new StackValidationException("투입할 재료 카드가 없습니다");
        }

        int totalScore = cards.stream().mapToInt(card -> card.getGrade().score()).sum();
        boolean affiliateBoost = todayVerifiedLogs.stream().anyMatch(SupplementLog::isAffiliateProduct);
        List<String> includedSupplements = todayVerifiedLogs.stream().map(SupplementLog::getProductName).toList();

        return new StackEvaluation(cards, totalScore, affiliateBoost, includedSupplements);
    }
}
