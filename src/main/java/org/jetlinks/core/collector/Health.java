package org.jetlinks.core.collector;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Health {

    private double health;

    private String reason;

    public static Health ok() {
        Health h = new Health();
        h.setHealth(1);
        h.setReason("ok");
        return h;
    }
}
