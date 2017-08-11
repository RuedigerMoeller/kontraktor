package org.nustaq.kontraktor

import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher
import org.nustaq.kontraktor.util.Log

/**
 * Created by ruedi on 31.07.17.
 */
fun main(args: Array<String>) {

//    var x:(Any?,err:Any?) -> Unit
//    x = { x,y ->};
//    println(x::class.java)

    var act : TestActor = Actors.AsActor(TestActor::class.java)

    TCPNIOPublisher( act, 7654 ).publish {
        actor -> Log.Info(null, "$actor has disconnected")
    }.then { x,err ->
        println("published $x $err")
    }
}