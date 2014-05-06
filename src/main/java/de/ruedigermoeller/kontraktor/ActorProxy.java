package de.ruedigermoeller.kontraktor;

/**
* Copyright (c) 2012, Ruediger Moeller. All rights reserved.
* <p/>
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
* <p/>
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
* <p/>
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
* <p/>
* Date: 03.01.14
* Time: 21:00
* To change this template use File | Settings | File Templates.
*/

import de.ruedigermoeller.kontraktor.impl.DispatcherThread;

/**
 * taging interface. Using Actors.Asacor() actually returns a proxy which enqueing each method call to the real actors queue.
 * This way one can determine wether an Object is the "real" actor or a proxy reference to it.
 * @param <T>
 */
public interface ActorProxy<T extends Actor> {

    /**
     * for internal use only
     * @param disp
     * @return
     */
    public void __setDispatcher(DispatcherThread disp);

    public DispatcherThread getDispatcher();
    public Actor getActor();

}
