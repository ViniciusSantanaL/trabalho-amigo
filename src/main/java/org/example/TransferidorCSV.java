package org.example;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import javax.xml.crypto.Data;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Scanner;

public class TransferidorCSV {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Digite o caminho completo para o arquivo CSV:");

        String csvFile = scanner.nextLine();
        Connection connection = DatabaseManager.getConnection();

        long startTime = System.nanoTime();

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {

            String[] headers = reader.readNext();
            if (headers == null) {
                throw new IllegalStateException("O arquivo CSV está vazio!");
            }

            StringBuilder sql = new StringBuilder("CREATE TABLE Dados_Bilionários (");
            for (String header : headers) {
                sql.append(header).append(" VARCHAR(255), ");
            }
            sql.delete(sql.length() - 2, sql.length()).append(")");
            System.out.println(sql);
            try (PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {
                pstmt.execute();
            }

            System.out.println("Tabela criada com sucesso!");

            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                StringBuilder insertSql = new StringBuilder("INSERT INTO Dados_Bilionários VALUES (");
                for (String value : nextLine) {
                    insertSql.append("?, ");
                }
                insertSql.delete(insertSql.length() - 2, insertSql.length()).append(")");

                try (PreparedStatement pstmt = connection.prepareStatement(insertSql.toString())) {
                    for (int i = 0; i < nextLine.length; i++) {
                        if (nextLine[i].matches("-?\\d+")) { // Inteiro
                            pstmt.setInt(i + 1, Integer.parseInt(nextLine[i]));
                        } else if (nextLine[i].matches("-?\\d+\\.\\d+")) { // Ponto flutuante
                            pstmt.setDouble(i + 1, Double.parseDouble(nextLine[i]));
                        } else if (nextLine[i].equalsIgnoreCase("true") || nextLine[i].equalsIgnoreCase("false")) { // Booleano
                            pstmt.setBoolean(i + 1, Boolean.parseBoolean(nextLine[i]));
                        } else { // String
                            pstmt.setString(i + 1, nextLine[i]);
                        }
                    }
                    System.out.println(insertSql);
                    pstmt.executeUpdate();
                }
            }

            System.out.println("Dados inseridos com sucesso!");

            System.out.println("------------------------");

            // Contar o número de registros
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Dados_Bilionários")) {
                if (rs.next()) {
                    System.out.println("Número de registros: " + rs.getInt(1));
                }
            }

            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Dados_Bilionários WHERE category = 'Technology'")) {
                if (rs.next()) {
                    System.out.println("Número de registros, com categoria 'Technology': " + rs.getInt(1));
                }
            }

        } catch (FileNotFoundException e) {
            System.out.println("Coordenadas do diretório errado, tente novamente");
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } catch (CsvValidationException e) {
            throw new RuntimeException(e);
        } finally {
            DatabaseManager.closeConnection();
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime);  // tempo em nanosegundos
        double seconds = (double)duration / 1_000_000_000.0; // convertendo para segundos

        System.out.println("Tempo consumido no processamento: " + seconds + " segundos");
    }
}
