import java.io.BufferedReader;
import java.io.FileReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class ClienteUDP {
    private DatagramSocket socket;
    private InetAddress servidorAddress;
    private int servidorPuerto;
    private String nombre;
    private Map<String, String> configuracion;
    private Map<String, String> topologia;
    private Scanner scanner;

    public ClienteUDP(String nombre, InetAddress servidorAddress, int servidorPuerto,
                      Map<String, String> configuracion, Map<String, String> topologia) {
        this.nombre = nombre;
        this.servidorAddress = servidorAddress;
        this.servidorPuerto = servidorPuerto;
        this.configuracion = configuracion;
        this.topologia = topologia;
        this.scanner = new Scanner(System.in);
    }

    public void iniciar() {
        try {
            socket = new DatagramSocket();

            while (true) {
                System.out.print("Ingrese el nombre del destinatario ('exit' para salir): ");
                String destinatarioNombre = scanner.nextLine().trim();

                if (destinatarioNombre.equalsIgnoreCase("exit")) {
                    break;
                }

                System.out.print("Mensaje: ");
                String mensaje = scanner.nextLine();

                enviarMensaje(destinatarioNombre, mensaje);
            }

            // Cerrar socket al salir
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void enviarMensaje(String destinatarioNombre, String mensaje) {
        try {
            String destinatarioIPPuerto = configuracion.get(destinatarioNombre);

            if (destinatarioIPPuerto != null) {
                // Construir el mensaje con el formato "nombre: mensaje"
                String mensajeCompleto = nombre + ": " + mensaje;
                byte[] datos = mensajeCompleto.getBytes();

                // Separar IP y puerto
                String[] partes = destinatarioIPPuerto.split(":");
                if (partes.length == 2) {
                    // Aquí puedes manejar la lógica cuando solo hay IP
                    InetAddress destinatarioAddress = InetAddress.getByName(partes[0]);
                    int destinatarioPuerto = Integer.parseInt(partes[1]);

                    // Crear el paquete UDP
                    DatagramPacket paquete = new DatagramPacket(datos, datos.length, destinatarioAddress, destinatarioPuerto);

                    // Enviar el paquete
                    socket.send(paquete);

                    System.out.println("Mensaje enviado a " + destinatarioNombre + " (" + destinatarioIPPuerto + ")");
                } else if (partes.length == 3) {
                    // Aquí puedes manejar la lógica cuando hay IP y puerto
                    InetAddress destinatarioAddress = InetAddress.getByName(partes[0]);
                    int destinatarioPuerto = Integer.parseInt(partes[2]);

                    // Crear el paquete UDP
                    DatagramPacket paquete = new DatagramPacket(datos, datos.length, destinatarioAddress, destinatarioPuerto);

                    // Enviar el paquete
                    socket.send(paquete);

                    System.out.println("Mensaje enviado a " + destinatarioNombre + " (" + destinatarioIPPuerto + ")");
                } else {
                    System.out.println("Formato incorrecto para el destinatario: " + destinatarioNombre);
                }
            } else {
                System.out.println("No se encontró la configuración para el destinatario: " + destinatarioNombre);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String obtenerIPDestinatario(String destinatarioNombre) {
        String destinatarioIPPuerto = configuracion.get(destinatarioNombre);

        if (destinatarioIPPuerto == null) {
            // Si no encontramos la IP directa, intentamos buscar en la topología
            String clienteIntermedio = topologia.get(nombre);

            while (clienteIntermedio != null) {
                destinatarioIPPuerto = configuracion.get(clienteIntermedio);

                if (destinatarioIPPuerto != null) {
                    break;
                }

                clienteIntermedio = topologia.get(clienteIntermedio);
            }
        }

        // Separar IP y puerto
        String[] partes = destinatarioIPPuerto.split(":");
        if (partes.length == 2) {
            return partes[0];  // Solo IP
        } else if (partes.length == 3) {
            // Aquí podrías manejar la situación con IP y puerto
            return partes[0];  // Solo IP
        } else {
            return null;  // Manejar el caso incorrecto
        }
    }

    private int obtenerPuertoDestinatario(String destinatarioNombre) {
        String destinatarioIPPuerto = configuracion.get(destinatarioNombre);

        // Separar IP y puerto
        String[] partes = destinatarioIPPuerto.split(":");
        if (partes.length == 2) {
            return Integer.parseInt(partes[1]);  // Solo puerto
        } else if (partes.length == 3) {
            // Aquí podrías manejar la situación con IP y puerto
            return Integer.parseInt(partes[2]);  // Solo puerto
        } else {
            return -1;  // Manejar el caso incorrecto
        }
    }

    public static void main(String[] args) {
        String nombreCliente = "JUAN";
        String rutaConfiguracion = "/home/eberzeta1/Escritorio/RedesRec/src/Configuracion";
        String rutaTopologia = "/home/eberzeta1/Escritorio/RedesRec/src/Conexion";

        try {
            InetAddress servidorAddress = InetAddress.getByName("localhost");  // Cambiar a la dirección del servidor
            int servidorPuerto = 30032;  // Cambiar al puerto del servidor

            Map<String, String> configuracion = cargarConfiguracion(rutaConfiguracion);
            Map<String, String> topologia = cargarTopologia(rutaTopologia);

            ClienteUDP cliente = new ClienteUDP(nombreCliente, servidorAddress, servidorPuerto, configuracion, topologia);
            cliente.iniciar();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        } catch (Exception e) {
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
        } catch (Exception e) {
            e.printStackTrace();
        }
        return topologia;
    }
}
