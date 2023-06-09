package io.lesson04.eventsourcing.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import io.lesson04.eventsourcing.MessageCarrier;
import io.lesson04.eventsourcing.Person;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;
@Component
public class MessageHandler {
    private final ConnectionFactory connectionFactory;
    private final DataSource dataSource;

    public MessageHandler(ConnectionFactory connectionFactory, DataSource dataSource) {
        this.connectionFactory = connectionFactory;
        this.dataSource = dataSource;
    }

    public void execute() throws IOException, TimeoutException, SQLException {
        while ((!Thread.currentThread().isInterrupted())) {
            readMessage(getMessage());
        }
    }

    private MessageCarrier getMessage() throws IOException, TimeoutException {
        MessageCarrier carrier = null;
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {
            String queueName = "queue";
            GetResponse message = channel.basicGet(queueName, true);
            if (message != null) {
                System.out.println("got message!");
                ObjectMapper mapper = new ObjectMapper();
                carrier = mapper.readValue(message.getBody(), MessageCarrier.class);
                System.out.println(carrier.getCommand());
            }
        }
        return carrier;
    }

    private void readMessage(MessageCarrier carrier) throws SQLException {
        if (carrier == null) {
            return;
        }
        if (carrier.getCommand().equals("save")) {
            save(carrier.getPerson());
        }
        if (carrier.getCommand().equals("delete")) {
            delete(carrier.getPerson());
        }
    }


    private void delete(Person person) throws SQLException {
            Long id = person.getId();
            if (!containsPerson(new Person(id, null, null, null))) {
                System.err.println("No person with id = " + id);
                return;
            }
            String sqlMessage = "delete from person where person_id = ?; ";
            try (java.sql.Connection connection = dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(sqlMessage)) {
                preparedStatement.setLong(1, id);
                preparedStatement.execute();
            }
        }


    private void save(Person person) throws SQLException {
        String sqlMessage = "insert into person (first_name, last_name, middle_name,person_id) values (?,?,?,?);";
        if (containsPerson(person)) {
            sqlMessage = "update person set first_name = ?, last_name = ?, middle_name = ? where person_id = ?;";
        }
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sqlMessage)) {
            preparedStatement.setString(1, person.getName());
            preparedStatement.setString(2, person.getLastName());
            preparedStatement.setString(3, person.getMiddleName());
            preparedStatement.setLong(4, person.getId());
            preparedStatement.execute();
        }
    }

    private boolean containsPerson(Person p) throws SQLException {
        String sqlMessage = "select person_id from person where person_id = ?;";
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sqlMessage)) {
            preparedStatement.setLong(1, p.getId());
            ResultSet resultSet = preparedStatement.executeQuery();
            return resultSet.next();
        }
    }
}
