import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Cliente2 {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private BufferedReader stdIn;

    private String nombre;
    private Map<String, String> configuracion;
    private Map<String, String> topologia;

    public Cliente2(String nombre, Map<String, String> configuracion, Map<String, String> topologia) {
        this.nombre = nombre;
        this.configuracion = configuracion;
        this.topologia = topologia;
    }

    public void start() {
        try {
            String ipPuerto = configuracion.get(nombre);
            if (ipPuerto != null) {
                String[] partes = ipPuerto.split(":");
                String host = partes[0];
                int port = Integer.parseInt(partes[1]);

                socket = new Socket(host, port);
                System.out.println("Conectado al servidor: " + socket);

                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                stdIn = new BufferedReader(new InputStreamReader(System.in));

                new Thread(this::leerRespuestasServer).start();

                while (true) {
                    System.out.print("Ingrese el nombre del destinatario ('exit' para salir): ");
                    String destinatarioNombre = stdIn.readLine().trim();

                    if (destinatarioNombre.equalsIgnoreCase("exit")) {
                        break;
                    }

                    String destinatarioIP = obtenerIPDestinatario(destinatarioNombre);

                    if (destinatarioIP != null) {
                        System.out.print("Mensaje: ");
                        String mensaje = stdIn.readLine();
                        out.println(nombre + ": " + mensaje);
                    } else {
                        System.out.println("No se encontró una ruta para el destinatario: " + destinatarioNombre);
                    }
                }

                in.close();
                out.close();
                socket.close();
                System.out.println("Desconectado del servidor");
            } else {
                System.out.println("No se encontró configuración para el cliente: " + nombre);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String obtenerIPDestinatario(String destinatarioNombre) {
        String destinatarioIP = configuracion.get(destinatarioNombre);

        if (destinatarioIP == null) {
            // Si no encontramos la IP directa, intentamos buscar en la topología
            String clienteIntermedio = topologia.get(nombre);

            while (clienteIntermedio != null) {
                destinatarioIP = configuracion.get(clienteIntermedio);

                if (destinatarioIP != null) {
                    break;
                }

                clienteIntermedio = topologia.get(clienteIntermedio);
            }
        }

        return destinatarioIP;
    }

    private void leerRespuestasServer() {
        try {
            String respuestaServidor;
            while ((respuestaServidor = in.readLine()) != null) {
                System.out.println("Respuesta del servidor: " + respuestaServidor);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Map<String, String> configuracion = cargarConfiguracion("/home/eberzeta1/Escritorio/RedesRec/src/Configuracion");
        Map<String, String> topologia = cargarTopologia("/home/eberzeta1/Escritorio/RedesRec/src/Conexion");

        Scanner scanner = new Scanner(System.in);
        System.out.print("INGRESE SU NOMBRE: ");
        String nombreCliente = scanner.nextLine().trim();

        Cliente2 cliente = new Cliente2(nombreCliente, configuracion, topologia);
        cliente.start();

        scanner.close();
    }

    private static Map<String, String> cargarConfiguracion(String rutaArchivo) {
        Map<String, String> configuracion = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            br.lines().forEach(line -> {
                String[] partes = line.split(":");
                String nombre = partes[0];
                String ipPuerto = partes[1] + ":" + partes[2];
                configuracion.put(nombre, ipPuerto);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return configuracion;
    }

    private static Map<String, String> cargarTopologia(String rutaArchivo) {
        Map<String, String> topologia = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            String line = br.readLine();
            if (line != null) {
                String[] partes = line.split("<->");
                for (int i = 0; i < partes.length - 1; i++) {
                    String clienteActual = partes[i];
                    String clienteConectado = partes[i + 1];
                    topologia.put(clienteActual, clienteConectado);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return topologia;
    }
}
