package it.pagopa.pn.deliverypushworkflow.service.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import static it.pagopa.pn.deliverypushworkflow.middleware.externalclient.pnclient.safestorage.PnSafeStorageClient.SAFE_STORAGE_URL_PREFIX;


@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtils {

    @NotNull
    public static String getKeyWithStoragePrefix(String key) {
        if(key.startsWith(SAFE_STORAGE_URL_PREFIX)){
            return key;
        }
        return SAFE_STORAGE_URL_PREFIX + key;
    }
}
