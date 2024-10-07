package com.pobluesky.voc.global.redisson;

public class LockKeyGenerator {

    public static String generateCollaborationKey(
            Long colReqId,
            Long colResId
    ) {
        Long firstId = Math.min(colReqId, colResId);
        Long secondId = Math.max(colReqId, colResId);

        String key = String.format("%d-%d", firstId, secondId);

        return key;
    }
}
