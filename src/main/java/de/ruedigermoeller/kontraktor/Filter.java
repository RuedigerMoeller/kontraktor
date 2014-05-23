package de.ruedigermoeller.kontraktor;

/**
 * Created by ruedi on 23.05.14.
 */
public interface Filter<IN,OUT> {

    public OUT filter( IN in );

}
