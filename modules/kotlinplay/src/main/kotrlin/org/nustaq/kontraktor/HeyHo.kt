package org.nustaq.kontraktor

import org.nustaq.kontraktor.annotations.CallerSideMethod
import org.nustaq.kontraktor.remoting.tcp.TCPClientConnector
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher
import org.nustaq.kontraktor.util.Log
import java.util.function.Consumer

/**
 * Created by ruedi on 19.05.17.
 */
open class TestActor : Actor<TestActor>() {

    private var test : Int? = 77;

    fun init() {
        test = 77;
    }

    fun xy( x:Int ) : IPromise<Int> {
        println("Hello $x")
        if ( x < 0 )
            return reject("BUG")
        return resolve(x+13)
    }

    fun regCB( cb: Callback<Any?> ) {
        cb.pipe(1)
        cb.pipe(2)
        cb.pipe(test)
        cb.finish()
    }

    private fun helper(x:Int) : Int = 2*x

    @CallerSideMethod
    fun workAround( cb: (res:Any?,err:Any?) -> Unit) {
        regCB( Callback { x,y -> cb(x,y) } )
    }

    fun testArr( arr: IntArray ) {
        arr.forEach { println(it) }
    }

    fun regCB1( cb: Callback<Int?> ) {
        cb.pipe(1)
        cb.pipe(2)
        cb.finish()
    }

    fun cons( cs: Consumer<Any?>) {

    }
}

fun main(args: Array<String>) {

//    var x:(Any?,err:Any?) -> Unit
//    x = { x,y ->};
//    println(x::class.java)

    var act : TestActor = Actors.AsActor(TestActor::class.java)

    TCPNIOPublisher( act, 7654 ).publish {
        actor -> Log.Info(null, "$actor has disconnected")
    }.await()

    TCPConnectable(TestActor::class.java, "localhost", 7654).connect<TestActor> {
        x, y -> println("$x $y")
    }.onResult { client ->
        client.init();
        client.xy( 17 ).then { x,y -> println("remote result $x" ) }
        client.regCB( Callback { x, any -> println(x) } )

        client.xy( -17 ).onResult {
            println("remote result $it" )
        }.onError {
            println("error $it")
        }

        client.regCB1( Callback { x, any -> println( x ) } )
        client.workAround {
            x,y -> println("work res $x $y")
        }
        client.testArr( intArrayOf(55,44,33,22))

        client.cons(Consumer { x -> println(x) })

        intArrayOf(55,44,33,22).map {  }


    }.onError {
        println("connection failed $it")
    }

}
