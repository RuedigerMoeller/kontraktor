package service.common;

import org.nustaq.serialization.annotations.Serialize;

import java.io.Serializable;

/**
 * Created by ruedi on 19/05/15.
 *
 * Ofc these classes can be defined in separate files, however this way versioning is easy (e.g. MyProtocol_V1)
 *
 */
public class MyProtocol {

    public static enum Sex {
        MALE,
        FEMALE,
        NO
    }

    public static class Person implements Serializable {

        final String name;
        final String preName;

        final int age;
        final Sex sex;

        public Person(String name, String preName, int age, Sex sex) {
            this.name = name;
            this.preName = preName;
            this.age = age;
            this.sex = sex;
        }

        public String getName() {
            return name;
        }

        public String getPreName() {
            return preName;
        }

        public int getAge() {
            return age;
        }

        public Sex getSex() {
            return sex;
        }

        @Override
        public String toString() {
            return "Person{" +
                       "name='" + name + '\'' +
                       ", preName='" + preName + '\'' +
                       ", age=" + age +
                       ", sex=" + sex +
                       '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;

            Person person = (Person) o;

            if (getAge() != person.getAge()) return false;
            if (getName() != null ? !getName().equals(person.getName()) : person.getName() != null) return false;
            if (getPreName() != null ? !getPreName().equals(person.getPreName()) : person.getPreName() != null)
                return false;
            return getSex() == person.getSex();

        }

        @Override
        public int hashCode() {
            int result = getName() != null ? getName().hashCode() : 0;
            result = 31 * result + (getPreName() != null ? getPreName().hashCode() : 0);
            result = 31 * result + getAge();
            result = 31 * result + (getSex() != null ? getSex().hashCode() : 0);
            return result;
        }
    }

}
