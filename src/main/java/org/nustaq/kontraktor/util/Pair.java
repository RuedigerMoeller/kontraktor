/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.util;

import java.io.Serializable;

/**
 * Created by ruedi on 03/05/15.
 *
 * A simple pair class. Though it has methods for mutation, it should be considered IMMUTABLE.
 * Mutation should be used for init/setup only and Pair will be treated like a real immutable by kontraktor.
 */
public class Pair<CAR, CDR> implements Serializable {

    CAR car;
    CDR cdr;

    public Pair(CAR CAR, CDR CDR) {
        this.car = CAR;
        this.cdr = CDR;
    }

    public Pair() {
    }

    public CAR getFirst() {
        return car;
    }

    public CDR getSecond() {
        return cdr;
    }

    public CAR car() {
        return car;
    }

    public CDR cdr() {
        return cdr;
    }

    public Pair car(final CAR car) {
        this.car = car;
        return this;
    }

    public Pair cdr(final CDR cdr) {
        this.cdr = cdr;
        return this;
    }

    @Override
    public String toString() {
        return "( "+car+", "+cdr+" )";
    }

    public boolean allNull() {
        return cdr == null && car == null;
    }
}
