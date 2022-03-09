package org.jetlinks.core.route;

import lombok.Getter;

@Getter
class DefaultMqttRoute implements MqttRoute {

    private final String topic;
    private final boolean upstream;
    private final boolean downstream;
    private final int qos;
    private final String group;
    private final String description;
    private final String example;

    DefaultMqttRoute(String topic,
                     boolean upstream,
                     boolean downstream,
                     int qos,
                     String group,
                     String description,
                     String example) {
        this.topic = topic;
        this.upstream = upstream;
        this.downstream = downstream;
        this.qos = qos;
        this.group = group;
        this.description = description;
        this.example = example;
    }


    static DefaultMqttRouteBuilder builder() {
        return new DefaultMqttRouteBuilder();
    }


    static class DefaultMqttRouteBuilder implements Builder {
        private String topic;
        private boolean upstream;
        private boolean downstream;
        private int qos;
        private String group;
        private String description;
        private String example;

        DefaultMqttRouteBuilder() {
        }

        public DefaultMqttRouteBuilder topic(String topic) {
            this.topic = topic;
            return this;
        }

        public DefaultMqttRouteBuilder upstream(boolean upstream) {
            this.upstream = upstream;
            return this;
        }

        public DefaultMqttRouteBuilder downstream(boolean downstream) {
            this.downstream = downstream;
            return this;
        }

        public DefaultMqttRouteBuilder qos(int qos) {
            this.qos = qos;
            return this;
        }

        public DefaultMqttRouteBuilder group(String group) {
            this.group = group;
            return this;
        }

        public DefaultMqttRouteBuilder description(String description) {
            this.description = description;
            return this;
        }

        public DefaultMqttRouteBuilder example(String example) {
            this.example = example;
            return this;
        }

        public DefaultMqttRoute build() {
            return new DefaultMqttRoute(topic, upstream, downstream, qos, group, description, example);
        }

        public String toString() {
            return "DefaultMqttRoute.DefaultMqttRouteBuilder(topic=" + this.topic + ", upstream=" + this.upstream + ", downstream=" + this.downstream + ", qos=" + this.qos + ", group=" + this.group + ", description=" + this.description + ", example=" + this.example + ")";
        }
    }
}
