package org.nustaq.kontraktor

import org.nustaq.kontraktor.remoting.tcp.TCPClientConnector
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher
import org.nustaq.kontraktor.util.Log

/**
 * Created by ruedi on 19.05.17.
 */
open class TestActor : Actor<TestActor>() {

    fun xy( x:Int ) : IPromise<Int> {
        println("Hello")
        return Actors.resolve(x+13)
    }

    fun regCB( cb: Callback<Any?> ) {
        cb.stream(1)
        cb.stream(2)
        cb.finish()
    }

    fun regCB1( cb: Callback<Int> ) {
        cb.stream(1)
        cb.stream(2)
        cb.finish()
    }
}

fun main(args: Array<String>) {
    var act : TestActor = Actors.AsActor(TestActor::class.java)
    act.xy(13).then { i, err -> println(i) }

    TCPNIOPublisher( act, 7654 ).publish {
        actor -> Log.Info(null, "$actor has disconnected")
    }.await()

    TCPConnectable(TestActor::class.java, "localhost", 7654).connect<TestActor> {
        x, y -> println("$x $y")
    }.then { client, err ->
        client.xy( 17 ).then { x,y -> println("remote result $x" ) }
        client.regCB( Callback { x, any -> println(x) } )

        client.xy( 17 ).onResult {
            println("remote result $it" )
        }.onError {
            println("error $it")
        }

        client.regCB1( Callback { x, any -> println( x ) } )
    }

}
