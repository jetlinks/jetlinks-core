package org.jetlinks.core.cluster.load;

public interface LoadBalancer<S> {


    static <S> LoadBalancer<S> create(){
        return new DefaultLoadBalancer<>();
    }

    void register(S server);

    void deregister(S server);

    S choose();

    S choose(Object key);

}
