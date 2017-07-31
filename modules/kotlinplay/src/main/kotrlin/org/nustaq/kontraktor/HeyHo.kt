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
        println("init called");
        test = 77;
    }

    fun xy( x:Int ) : IPromise<Int> {
        println("Server: Hello $x")
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