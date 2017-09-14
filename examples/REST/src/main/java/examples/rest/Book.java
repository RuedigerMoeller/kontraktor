package examples.rest;

import java.io.Serializable;

public class Book implements Serializable {
    String title;
    String id;
    String author;

    public String getTitle() {
        return title;
    }

    public String getId() {
        return id;
    }

    public String getAuthor() {
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

    public Book author(String author) {
        this.author = author;
        return this;
    }
}
