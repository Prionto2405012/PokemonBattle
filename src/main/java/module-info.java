module com.example.pokemonbattle {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires transitive javafx.graphics;
    requires transitive javafx.media;
    requires java.logging;
    requires java.sql;          // For SQLite database operations
    requires java.desktop;      // javax.imageio — GIF frame decoding
    requires javafx.swing;      // SwingFXUtils — BufferedImage → WritableImage
    requires com.google.gson;

    opens com.example.pokemonbattle to javafx.fxml;
    opens com.example.pokemonbattle.controller to javafx.fxml;
    opens com.example.pokemonbattle.util to javafx.fxml;
    opens com.example.pokemonbattle.model to javafx.fxml, com.google.gson;
    exports com.example.pokemonbattle;
    exports com.example.pokemonbattle.controller;
    exports com.example.pokemonbattle.model;
    exports com.example.pokemonbattle.util;
}