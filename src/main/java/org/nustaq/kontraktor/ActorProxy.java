package org.nustaq.kontraktor;

/**
* Copyright (c) 2012, Ruediger Moeller. All rights reserved.
*
* This library is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 2.1 of the License, or (at your option) any later version.
*
* This library is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this library; if not, write to the Free Software
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
* MA 02110-1301  USA
*
* Date: 03.01.14
* Time: 21:00
* To change this template use File | Settings | File Templates.
*/

/**
 * tagging interface. 
 * Using Actors.AsActor() actually returns a proxy which enqueues each method call to the 'real' actor instance.
 * This way one can determine wether an Object is the "real" actor or a proxy reference to it.
 * Another way to detect this is actor.getActor() == actor .
 * @param <T>
 */
public interface ActorProxy<T extends Actor> {

    public Actor getActor();

}
