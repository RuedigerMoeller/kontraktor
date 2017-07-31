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
        client.tell("init"); // works
        client.init();       // DOES NOT WORK !!!!!!!!!!!!!

        client.ask("xy", 17 ).then { x,y -> println("remote result $x" ) } // works

        client.xy( 17 ).then { x,y -> println("remote result $x" ) }  // does not work !!!!!
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