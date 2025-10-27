package org.example;

import javazoom.jl.player.Player;
import java.io.FileInputStream;

public class ControlFlujo {
    public static void main(String[] args) {
        try {
            FileInputStream archivo = new FileInputStream("C:\\Users\\esteb\\Documents\\6to_sem\\AplicacionesRed\\programasEjemplos\\ArchivoMP3\\src\\RH.mp3");
            Player reproductor = new Player(archivo);
            reproductor.play(); // Reproduce todo el archivo
        } catch (Exception e) {
            System.out.println("Error al reproducir el MP3: " + e.getMessage());
        }
    }
}
