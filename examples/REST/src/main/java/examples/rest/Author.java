package examples.rest;

import org.nustaq.kontraktor.remoting.base.JsonMapable;

import java.util.Date;
import java.util.List;

public class Author implements JsonMapable {
    String firstName;
    String lastName;
    long birthDate;
    Date testDate;

    List<Book> books;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public long getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(long birthDate) {
        this.birthDate = birthDate;
    }

    public Date getTestDate() {
        return testDate;
    }

    public void setTestDate(Date testDate) {
        this.testDate = testDate;
    }


    public Author firstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public Author lastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public Author birthDate(long birthDate) {
        this.birthDate = birthDate;
        return this;
    }

    public Author testDate(Date testDate) {
        this.testDate = testDate;
        return this;
    }

    public void setBooks(List<Book> books) {
        this.books = books;
    }

    public List<Book> getBooks() {
        return books;
    }
}
