### Operators

**!** - unary not

**+** - add / concat

**-** - subtract / remove string from end if matches

\* - multiply 

**/** - divide

\** - a contains b case insensitive, also works for arrays like prop ** [1,2,3] or [1,2,3] ** 2

\*> - a endswith b case insensitive

\<* - a startswith b case insnensitive

**==** - equals

**!=** - non equal

**&&** - conditional and

**||** - conditional or

**^** - xor

**<,<=,>,>=** - as usual, compareTo for Strings


### functions

**age(a,b)** - returns MS timestamp based on now. e.g. *age( 1, 'days' )* . units: ms,sec,min,hour,day,week,month,year

**now()** - current time millies

**lower** - to lower case

**upper** - to upper case

**exists** - not null or 0 or ""

**isEmpty** - null, 0 or empty string/array

**length** - lenth of array

### special attributes

**_key** - key of a record

**_lastModified** - record lastModified as long ms

