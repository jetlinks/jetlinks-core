package org.jetlinks.core.route;

public interface WebsocketRoute extends Route {

    default String getPath() {
        return getAddress();
    }

    static Builder builder() {
        return DefaultWebsocketRoute.builder();
    }

    interface Builder {
        Builder group(String group);

        Builder path(String address);

        Builder description(String description);

        Builder example(String example);

        WebsocketRoute build();
    }
}
