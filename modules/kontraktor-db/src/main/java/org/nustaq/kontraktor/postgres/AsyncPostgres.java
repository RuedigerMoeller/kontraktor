package org.nustaq.kontraktor.postgres;

import com.github.pgasync.*;
import org.nustaq.kontraktor.*;

import java.util.function.*;
import java.util.stream.*;

/**
 * Created by moelrue on 17.06.2015.
 */
public class AsyncPostgres extends Actor<AsyncPostgres> {


    public static void main(String[] args) {
        Db db = new ConnectionPoolBuilder()
                .hostname("localhost")
                .port(5432)
                .database("mydb")
                .username("postgres")
                .password("admin")
                .poolSize(20)
                .build();
        Consumer<Throwable> onError = error -> error.printStackTrace();

        db.begin(transaction -> {
            transaction.query("select * from public.mtab",
                    result -> {
                        System.out.println("Result is " + result);
                        transaction.commit(() -> System.out.println("Transaction committed"), onError);
                    },
                    error -> System.err.println("Error:"+error));
        }, onError);


        db.begin(transaction -> {
            IntStream.range(100,200).forEach( i -> {
                transaction.query("insert into public.MTab (ID,Name,Age,Rate,Sex) values ("+i+", 'The Moeller', 13 , '1.50','M')",
                        result -> {
                            System.out.printf("Result is " + result);
                            transaction.commit(() -> System.out.println("Transaction committed"), onError);
                        },
                        error -> System.err.println("Error:"+error));
            });
        }, onError);
    }
}
