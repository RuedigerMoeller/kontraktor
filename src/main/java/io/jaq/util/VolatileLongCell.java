package io.jaq.util;
/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author: Nitsan Wakart
 */
import static io.jaq.util.UnsafeAccess.UNSAFE;

abstract class VolatileLongCellP0 {
    long p0, p1, p2, p3, p4, p5, p6;
    long p10, p11, p12, p13, p14, p15, p16, p17;
}

abstract class VolatileLongCellValue extends VolatileLongCellP0 {
    protected volatile long value;
}

public final class VolatileLongCell extends VolatileLongCellValue {
    long p0, p1, p2, p3, p4, p5, p6;
    long p10, p11, p12, p13, p14, p15, p16, p17;
    private final static long VALUE_OFFSET;
    static {
        try {
            VALUE_OFFSET = UNSAFE.objectFieldOffset(VolatileLongCellValue.class
                    .getDeclaredField("value"));
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public VolatileLongCell() {
        this(0L);
    }

    public VolatileLongCell(long v) {
        lazySet(v);
    }

    public void lazySet(long v) {
        UnsafeAccess.UNSAFE.putOrderedLong(this, VALUE_OFFSET, v);
    }

    public void set(long v) {
        this.value = v;
    }

    public long get() {
        return this.value;
    }
}
