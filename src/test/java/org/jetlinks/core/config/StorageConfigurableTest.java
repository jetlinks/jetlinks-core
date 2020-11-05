package org.jetlinks.core.config;

import org.jetlinks.core.Configurable;
import org.jetlinks.core.Value;
import org.jetlinks.core.Values;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;

public class StorageConfigurableTest {

    @Test
    public void test() {

        InMemoryConfigStorage storage = new InMemoryConfigStorage();
        storage.setConfig("test", 1)
               .block();
        ;

        InMemoryConfigStorage parent = new InMemoryConfigStorage();
        Mono.zip(
                parent.setConfig("test2", 1),
                parent.setConfig("test3", 1)
        ).block();

        StorageConfigurable configurable = new StorageConfigurable() {
            @Override
            public Mono<ConfigStorage> getReactiveStorage() {
                return Mono.just(storage);
            }

            @Override
            public Mono<Configurable> getParent() {
                return Mono.just((StorageConfigurable) () -> Mono.just(parent));
            }
        };

        configurable.getConfig("test")
                    .map(Value::get)
                    .as(StepVerifier::create)
                    .expectNext(1)
                    .verifyComplete();

        configurable.getConfigs("test","test2","test3","test4")
                    .map(Values::getAllValues)
                    .as(StepVerifier::create)
                    .expectNext(new HashMap<String, Object>(){
                        {
                            put("test",1);
                            put("test2",1);
                            put("test3",1);
                        }
                    })
                    .verifyComplete();
    }

}