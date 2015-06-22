/*
Kontraktor-Http Copyright (c) Ruediger Moeller, All rights reserved.

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

package org.nustaq.kontraktor.remoting.http.javascript.minbingen;

import org.nustaq.kontraktor.annotations.RemoteActorInterface;
import org.nustaq.serialization.FSTClazzInfo;

import java.util.List;

/**
 * Created by ruedi on 01.10.14.
 */
public class GenClazzInfo {
	FSTClazzInfo clzInfo;
    List<MsgInfo> msgs;

	public GenClazzInfo(FSTClazzInfo clzInfo) {
		this.clzInfo = clzInfo;
		init();
	}

	private void init() {

	}

    public FSTClazzInfo getClzInfo() {
        return clzInfo;
    }

    public void setMsgs(List<MsgInfo> msgs) {
        this.msgs = msgs;
    }

    public List<MsgInfo> getMsgs() {
        return msgs;
    }

    public boolean isActor() {
        return msgs != null;
    }

	// actor definition to be implemented on client
	public boolean isClientSide() {
		return clzInfo.getClazz().getAnnotation(RemoteActorInterface.class) != null;
	}
}
