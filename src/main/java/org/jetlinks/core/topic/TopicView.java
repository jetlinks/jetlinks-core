package org.jetlinks.core.topic;

import lombok.Getter;
import lombok.Setter;

import java.util.Collection;
import java.util.Set;

@Getter
@Setter
public class TopicView {

    private String part;

    private Collection<TopicView> children;

    private Set<?> subscribers;
}
