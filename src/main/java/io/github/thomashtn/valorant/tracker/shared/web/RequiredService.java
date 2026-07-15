package io.github.thomashtn.valorant.tracker.shared.web;

import io.github.thomashtn.valorant.tracker.shared.exception.FeatureNotImplementedException;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Resolves optional application-service implementations used by controller scaffolding.
 */
public final class RequiredService {

    private RequiredService() {
        // Utility class: instantiation is intentionally disabled.
    }

    /**
     * Returns the available service implementation or reports that the feature is unfinished.
     *
     * @param provider Spring provider containing zero or one service implementation
     * @param featureName human-readable feature name included in the error response
     * @param <T> service contract type
     * @return resolved service implementation
     * @throws FeatureNotImplementedException when no implementation is registered
     */
    public static <T> T get(ObjectProvider<T> provider, String featureName) {
        T service = provider.getIfAvailable();
        if (service == null) {
            throw new FeatureNotImplementedException(featureName);
        }
        return service;
    }
}
