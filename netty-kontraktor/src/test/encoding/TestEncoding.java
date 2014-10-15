package encoding;

import org.nustaq.kontraktor.annotations.GenRemote;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;

import java.util.*;

/**
 * Created by ruedi on 07.10.14.
 */
@GenRemote
public class TestEncoding extends Actor<TestEncoding> {

	public Future<Object[]> numbers( byte by, short sh, char ch, int integ, double f, double d ) { // long ommited because of JS
		return new Promise<>(new Object[] {by,sh,ch,integ,f,d});
	}

	public Future<Object[]> arrays( Object oa[], String str[], byte by[], short sh[], char ch[], int integ[], double d[] ) {
		return new Promise( new Object[]{ oa, str, by, sh, ch, integ, d } );
	}

	public Future<List<List>> lists( ArrayList al, List li ) {
		ArrayList<List> res = new ArrayList<>();
		res.add(al); res.add(li);
		return new Promise<>( res );
	}

	public Future<Map> hmap( Map mp ) {
		return new Promise<>(mp);
	}

}
