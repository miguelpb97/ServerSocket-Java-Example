package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.*;
import java.util.Date;

public class Cliente {
	static final String HOST = "192.168.1.10"; // IP de la maquina virtual que alojaba el servidor
	static final int Puerto = 3000;

	public Cliente() {
		try {
			// Establecer conexión con el servidor pasandle la ip y el puerto
			Socket skCliente = new Socket(HOST, Puerto);

			// Crear flujos de entrada/salida para comunicarse con el servidor
			DataInputStream flujoEntrada = new DataInputStream(skCliente.getInputStream());
			DataOutputStream flujoSalida = new DataOutputStream(skCliente.getOutputStream());
			
			// Creamos un buffered reader al que le pararemos un imputstreamreader para
			// recoger por teclado el numero que le epdiremos al usuario
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

			// Variable que controla los estados del servidor
			int estado = 1;
			
			String comando = null;
			String nombreArchivoGet = null;
			String nombreArchivoCat = null;
			// Bucle que se repetira mientras el estado sea distinto a -1
			do {
				switch (estado) {
				case 1:
					System.out.println(flujoEntrada.readUTF()); // Mostrar mensaje del servidor
					comando = br.readLine(); // leemos el comando por teclado
					flujoSalida.writeUTF(comando); // Enviar comando al servidor
					// si el comando es igual a...
					if (comando.equals("ls")) {
						// pasamos al estado 2
						estado = 2;
					} else if (comando.equals("cat")) {
						// pasamos al estado 3
						estado = 3;
					} else if (comando.equals("get")) {
						// pasamos al estado 5
						estado = 5;
					} else if (comando.equals("time")) {
						// pasamos al estado 7
						estado = 7;
					} else if (comando.equals("stop")) {
						// pasamos al estado -1
						estado = -1;
					}
					break;
				case 2:
					// leemos el string que contiene la lista de ficheros concatenada en uan sola linea
					String ficheros = flujoEntrada.readUTF();
					//Separamos el string con ";"
					String archivos[] = ficheros.split(";");
					for (int i = 0; i < archivos.length; i++) {
						// mostramos al cliente los archivos
						System.out.println(i + " " + archivos[i]);
					}
					// pasamos al estado 1
					estado = 1;
					break;
				case 3:
					System.out.println(flujoEntrada.readUTF());
					nombreArchivoCat = br.readLine();
					flujoSalida.writeUTF(nombreArchivoCat);
					// pasamos al estado 4
					estado = 4;
					break;
				case 4:
					// al igual que para el listado de archivos, hacemos lo mismo para cada linea del documento
					String texto = flujoEntrada.readUTF();
					//Separamos el string con ";"
					String lineas[] = texto.split(";");
					for (int i = 0; i < lineas.length; i++) {
						// mostramos cada linea del archivo
					    System.out.println(i + " " + lineas[i]);
					}
					// pasamos al estado 1
					estado = 1;
					break;
				case 5:
					// Mostramos el mensaje que nos manda el servidor
					System.out.println(flujoEntrada.readUTF());
					// Leemos por teclado el nombre del archivo
					nombreArchivoGet = br.readLine();
					// Le pasamos el nombre del archivo al servidor
					flujoSalida.writeUTF(nombreArchivoGet);
					// pasamos al estado 6
					estado = 6;
					break;
				case 6:
					try {
						// Obtenemos el nombre del archivo
						String nombreArchivo = flujoEntrada.readUTF().toString();
						// Obtenemos el tamaño del archivo
						int tam = flujoEntrada.readInt();
						// Mostramos un mensaje al cliente
						System.out.println("Recibiendo archivo " + nombreArchivo);
						// Creamos flujo de salida y entrada
						FileOutputStream fos = new FileOutputStream(nombreArchivoGet);
						BufferedOutputStream out = new BufferedOutputStream(fos);
						BufferedInputStream in = new BufferedInputStream(flujoEntrada);
						// Creamos el array de bytes para leer los datos del archivo
						byte[] buffer = new byte[tam];
						// Obtenemos el archivo mediante la lectura de bytes enviados
						for (int i = 0; i < buffer.length; i++) {
							buffer[i] = (byte) in.read();
						}
						// Escribimos el archivo
						out.write(buffer);
						// Cerramos los flujos
						out.flush();
						out.close();
						in.close();
						// Mostramos Un mensaje al usuario
						System.out.println("Archivo Recibido " + nombreArchivo);
					} catch (Exception e) {
						System.err.println(e.getMessage());
					}
					// volvemos al estado 1
					estado = 1;
					break;
				case 7:
					// Obtenemos el tiempo del cliente
					long tiempoCliente = (new Date()).getTime();
					// Recibimos el tiempo que nos pasa el servidor
					long tiempoServidor = flujoEntrada.readLong();
					// calculamos la diferencia entre ambos tiempos
					long resultado = tiempoCliente - tiempoServidor;
					// mostramos al cliente el resultado
					System.out.println("Tiempo de respuesta: " + resultado + " ms");
					// volvemos al estado 1
					estado = 1;
					break;
				}
				// Si el comando es igual a exit o stop el estado pasa a -1
				if (comando.equals("exit")) {
					// pasamos al estado -1
					estado = -1;
				}
			} while (estado != -1);
			// cerramos los socket y los flujos de entrada/salida
			skCliente.close();
			flujoEntrada.close();
			flujoSalida.close();
			br.close();
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public static void main(String[] arg) {
		new Cliente();
	}
}