package org.jetlinks.core.metadata.types;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GeoPoint implements Serializable {

    //经度
    private double latitude;

    //纬度
    private double longitude;

    @Override
    public String toString() {
        return latitude + "," + longitude;
    }
}
