package examples.rest;

import org.nustaq.kontraktor.remoting.base.JsonMapable;

import java.io.Serializable;

public class Book implements Serializable, JsonMapable {
    String title;
    String id;
    Author author;

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public Author getAuthor() {
        return author;
    }

    public Book title(String title) {
        this.title = title;
        return this;
    }

    public Book id(String id) {
        this.id = id;
        return this;
    }

    public Book author(Author author) {
        this.author = author;
        return this;
    }
}
