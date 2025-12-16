package main;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Servidor extends Thread {
	static final int puerto = 3000;
	static String rutaServidor = new File(System.getProperty("user.dir")).getAbsolutePath();
	static ServerSocket skServidor;
	Socket skCliente;

	public Servidor(Socket sCliente) {
		skCliente = sCliente;
	}

	public static void main(String[] arg) {
		try {
			// Inicio el servidor en el puerto
			skServidor = new ServerSocket(puerto);
			System.out.println("Servidor Iniciado. Escucho el puerto: " + puerto);
			while (true) {
				// Se conecta un cliente
				Socket skCliente = skServidor.accept();
				System.out.println("Cliente conectado. IP:" + skCliente.getInetAddress());
				// Iniciamos el Thread del servidor
				new Servidor(skCliente).start();
			}
		} catch (SocketException e) {
			System.err.println(e.getMessage());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public void run() {
		try {
			// Cramos los flujos de entrada y salida que usaremos, y los string que usaremos
			// para guardar informacion recogida por teclado.
			DataOutputStream flujoSalida = new DataOutputStream(skCliente.getOutputStream());
			DataInputStream flujoEntrada = new DataInputStream(skCliente.getInputStream());
			String comandoLeido = null;
			String nombreArchivoGet = null;
			String nombreArchivoCat = null;
			int estado = 1;
			do {
				switch (estado) {
				case 1:
					// le pasamos un mesaje al flujo de salida pidiendo el numero, que sera el
					// mensaje que recibira el cliente
					flujoSalida.writeUTF("Cmd: ");
					// Leemos el flujo de entrada del cliente y lo pasamos a una variable
					comandoLeido = flujoEntrada.readUTF();
					// Si el comando leido del cliente es igual...
					if (comandoLeido.equals("ls")) {
						// pasamos al estado 2
						estado = 2;
					} else if (comandoLeido.equals("cat")) {
						// pasamos al estado 3
						estado = 3;
					} else if (comandoLeido.equals("get")) {
						// pasamos al estado 5
						estado = 5;
					} else if (comandoLeido.equals("time")) {
						// pasamos al estado 7
						estado = 7;
					} else if (comandoLeido.equals("exit")) {
						// cerramos la conexion del socket al cliente
						skCliente.close();
					}
					break;
				case 2:
					// le pasamos la ruta del servidor al file que usaremos para listar los archivos
					// de dicho directorio
					File carpeta = new File(rutaServidor);
					String texto = "";
					// creo un simpeldateformatter para recoger la fecha de mofidicacion de los
					// archivos que listaremos
					SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
					// creamos un array unidimensional de file donde podremos la lista de los
					// archivos y/o directorios de la ruta del servidor
					File[] archivos = carpeta.listFiles();
					if (archivos != null) {
						for (int i = 0; i < archivos.length; i++) {
							if (archivos[i].isDirectory()) {
								// concatenamos cada liea con un ; al final que usaremos como delimitador para
								// mostrarlo en el cliente, asi enviamos cada linea encontrada en una sola que
								// luego splitearemos en el cliente
								texto = texto + archivos[i].getName() + " - " + archivos[i].length() + " B "
										+ " - Fecha mod: " + sdf.format(archivos[i].lastModified()) + ";";
							} else if (!archivos[i].isDirectory()) {
								// los items que encuentre que no sean directorios en mi caso he añadido un
								// espacio en blanco al principio de los mismos para identificarlos como
								// archivos
								texto = texto + " " + archivos[i].getName() + " - " + archivos[i].length() + " B "
										+ " - Fecha mod: " + sdf.format(archivos[i].lastModified()) + ";";
							}
						}
						// escribimos las lineas concatenadas en un string y lo pasamos por el flujo de
						// salida
						flujoSalida.writeUTF(texto);
					}
					estado = 1;
					break;
				case 3:
					// le mandamos al usuario el mesaje
					flujoSalida.writeUTF("Introduce el nombre del archivo del servidor a mostrar: ");
					// recogemos el nombre del archivo
					nombreArchivoCat = flujoEntrada.readUTF();
					// pasamos al estado 4 que este sera encargado de mostrar el contenido del
					// archivo, es decir pasarlo por el flujo de salida
					estado = 4;
					break;
				case 4:
					// Crea un lector de archivo para leer las líneas
					BufferedReader fr = new BufferedReader(new FileReader(nombreArchivoCat));
					String linea;
					String lineas = "";
					// para cada linea distinta a null
					while ((linea = fr.readLine()) != null) {
						// concatenamos cada linea en el string y al final de cad aun añadimos un ; para
						// delimitarla y asi splitear en el cliente.
						lineas = lineas + linea + ";";
					}
					// enviamos los datos por el flujo de salida
					flujoSalida.writeUTF(lineas);
					// pasamos al estado 1
					estado = 1;
					break;
				case 5:
					flujoSalida.writeUTF("Introduce el nombre del archivo del servidor a obtener: ");
					nombreArchivoGet = flujoEntrada.readUTF();
					estado = 6;
					break;
				case 6:
					try {
						// Creamos el archivo que vamos a enviar
						File archivo = new File(nombreArchivoGet);
						// Obtenemos el tamaño del archivo
						int tamañoArchivo = (int) archivo.length();
						// Enviamos el nombre del archivo
						flujoSalida.writeUTF(archivo.getName());
						// Enviamos el tamaño del archivo
						flujoSalida.writeInt(tamañoArchivo);
						// Creamos flujo de entrada para realizar la lectura del archivo en bytes
						FileInputStream fis = new FileInputStream(nombreArchivoGet);
						BufferedInputStream bis = new BufferedInputStream(fis);
						// Creamos el flujo de salida para enviar los datos del archivo en bytes
						BufferedOutputStream bos = new BufferedOutputStream(flujoSalida);
						// Creamos un array de tipo byte con el tamaño del archivo
						byte[] buffer = new byte[tamañoArchivo];
						// Leemos el archivo y lo introducimos en el array de bytes
						bis.read(buffer);
						// Realizamos el envio de los bytes que conforman el archivo
						for (int i = 0; i < buffer.length; i++) {
							bos.write(buffer[i]);
						}
						// Cerramos los flujos, en 
						bis.close();
						bos.close();
					} catch (Exception e) {
						System.err.println(e.getMessage());
					}
					// Volvemos al estado 1
					estado = 1;
					break;
				case 7:
					// Recogemos el tiempo del servidor
					long tiempoServidor = (new Date()).getTime();
					// Lo enviamos como long pro el flujo de salida
					flujoSalida.writeLong(tiempoServidor);
					// Volvemos al estado 1
					estado = 1;
					break;
				}
				if (comandoLeido.equals("stop")) {
					// pasamos al estado -1
					estado = -1;
				}
			} while (estado != -1);
			// Cerramos la conexion del cliente.
			skCliente.close();
			skServidor.close();
		} catch (SocketException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
	}
}