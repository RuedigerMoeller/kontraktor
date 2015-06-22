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

import java.lang.reflect.Parameter;

/**
 * Created by ruedi on 30.09.14.
 */
public class MsgInfo {

    Parameter[] parameters;
    Class params[];
	String name;
	String returnType;

	public MsgInfo(Class[] params, String name, String returnType, Parameter[] parameters) {
		this.params = params;
		this.name = name;
		this.returnType = returnType;
        this.parameters = parameters;
	}

	public Class[] getParams() {
		return params;
	}

	public void setParams(Class[] params) {
		this.params = params;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getReturnType() {
		return returnType;
	}

	public void setReturnType(String returnType) {
		this.returnType = returnType;
	}

    public Parameter[] getParameters() {
        return parameters;
    }
	public boolean hasFutureResult() {
		return returnType.equals("IPromise");
	}
}
