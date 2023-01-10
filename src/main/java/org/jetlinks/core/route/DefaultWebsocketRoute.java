package org.jetlinks.core.route;

import lombok.Getter;

@Getter
class DefaultWebsocketRoute implements WebsocketRoute {
    private final String group;
    private final String address;
    private final String description;
    private final String example;


    DefaultWebsocketRoute(String group,
                          String address,
                          String description,
                          String example) {
        this.group = group;
        this.address = address;
        this.description = description;
        this.example = example;
    }

    public static DefaultWebsocketRouteBuilder builder() {
        return new DefaultWebsocketRouteBuilder();
    }

    static class DefaultWebsocketRouteBuilder implements Builder {
        private String group;
        private String address;
        private String description;
        private String example;

        DefaultWebsocketRouteBuilder() {
        }

        public DefaultWebsocketRouteBuilder group(String group) {
            this.group = group;
            return this;
        }

        public DefaultWebsocketRouteBuilder path(String path) {
            this.address = path;
            return this;
        }

        public DefaultWebsocketRouteBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DefaultWebsocketRouteBuilder example(String example) {
            this.example = example;
            return this;
        }

        public DefaultWebsocketRoute build() {
            return new DefaultWebsocketRoute(group, address, description, example);
        }

        public String toString() {
            return "DefaultWebsocketRoute.DefaultWebsocketRouteBuilder(group=" + this.group + ", address=" + this.address + ", description=" + this.description + ", example=" + this.example + ")";
        }
    }
}
