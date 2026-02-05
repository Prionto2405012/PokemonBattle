module com.example.pokemonbattle {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires transitive javafx.graphics;
    requires java.logging;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires com.almasb.fxgl.all;

    opens com.example.pokemonbattle to javafx.fxml;
    opens com.example.pokemonbattle.controller to javafx.fxml;
    opens com.example.pokemonbattle.util to javafx.fxml;
    exports com.example.pokemonbattle;
    exports com.example.pokemonbattle.controller;
    exports com.example.pokemonbattle.util;
}