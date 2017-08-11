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

    open fun init() {
        println("init called");
        test = 77;
    }

    open fun xy( x:Int ) : IPromise<Int> {
        println("Server: xy $x")
        if ( x < 0 )
            return reject("BUG")
        return resolve(x+13)
    }

    open fun regCB( cb: Callback<Any?> ) {
        println("Server: regCB")
        cb.pipe(1)
        cb.pipe(2)
        cb.pipe(test)
        cb.finish()
    }

    private fun helper(x:Int) : Int = 2*x

    @CallerSideMethod
    open fun workAround( cb: (res:Any?,err:Any?) -> Unit) {
        println("Server: workAround")
        regCB( Callback { x,y -> cb(x,y) } )
    }

    open fun testArr( arr: IntArray ) {
        println("Server: testArr")
        arr.forEach { println(it) }
    }

    open fun regCB1( cb: Callback<Int?> ) {
        println("Server: regCB1")
        cb.pipe(1)
        cb.pipe(2)
        cb.finish()
    }

    open fun cons( cs: Consumer<Any?>) {

    }
}