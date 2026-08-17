package com.elixircodex.backend.stack;

public record SupplementVerifyResponse(Long supplementLogId, String productName, int confidenceScore,
                                        boolean isVerified, boolean isAffiliateProduct) {
}
