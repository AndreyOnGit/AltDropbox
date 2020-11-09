package server;

import java.util.Objects;

public interface AuthService {
    Record findRecord(String login, String password);

    class Record {
        private long id;
        private String login;
        private String password;

        public Record(long id, String login, String password) {
            this.id = id;
            this.login = login;
            this.password = password;
        }


        public long getId() {
            return id;
        }

        public String getLogin() {
            return login;
        }

        public String getPassword() {
            return password;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Record record = (Record) o;
            return id == record.id &&
                    login.equals(record.login) &&
                    password.equals(record.password);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, login, password);
        }

        @Override
        public String toString() {
            return "Record{" +
                    "id=" + id +
                    ", login='" + login + '\'' +
                    ", password='" + password + '\'' +
                    '}';
        }
    }
}
