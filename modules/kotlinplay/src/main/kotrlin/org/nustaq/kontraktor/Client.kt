package org.nustaq.kontraktor

import org.nustaq.kontraktor.remoting.tcp.TCPConnectable
import java.lang.reflect.Method
import java.util.function.Consumer

/**
 * Created by ruedi on 31.07.17.
 */
 fun main(args: Array<String>) {

    TCPConnectable(TestActor::class.java, "localhost", 7654).connect<TestActor> {
        x, y -> println("$x $y")
    }.onResult { client ->
        client.init();

        client.ask("xy", 17 ).then { x,y -> println("client remote result $x" ) }

        client.xy( 17 ).then { x,y -> println("client remote result $x" ) }
        client.regCB( Callback { x, any -> println(x) } )

        client.xy( -17 ).onResult {
            println("client remote result $it" )
        }.onError {
            println("client error $it")
        }

        client.regCB1( Callback { x, any -> println( x ) } )
        client.workAround {
            x,y -> println("client work res $x $y")
        }
        client.testArr( intArrayOf(55,44,33,22))

        // note: kotlin lambdas cannot be serialized as they apparently
        // still contain an outer "this" reference. In java 8 a pure lambda does not refer to outer 'this'
//        client.cons(Consumer { x -> println(x) })

        intArrayOf(55,44,33,22).map {  }


    }.onError {
        println("connection failed $it")
    }
}