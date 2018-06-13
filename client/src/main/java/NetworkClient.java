import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;

import java.io.IOException;
import java.net.Socket;

/*Класс содержит методы для работы сетью*/
public class NetworkClient {

    private static NetworkClient ourInstance = new NetworkClient();

    private Socket clientSocket;
    private ObjectDecoderInputStream odis;
    private ObjectEncoderOutputStream oeos;

    /*В поле хранится статус подключения к серверу*/
    private boolean isConnected;

    public static NetworkClient getInstance() {
        return ourInstance;
    }

    private NetworkClient() {
    }

    /*Выполняем подключение на заданный адрес и порт сервера
     * Возвращаем результат операции*/
    public boolean connect(String serverAddress, int serverPort) {
        try {
            clientSocket = new Socket(serverAddress, serverPort);
            oeos = new ObjectEncoderOutputStream(clientSocket.getOutputStream());
            odis = new ObjectDecoderInputStream(clientSocket.getInputStream());
            isConnected = true;
        } catch (IOException e) {
            e.printStackTrace();
            close();
        }
        return isConnected;
    }

    /*Возвращаем текущий статус поключения*/
    public boolean isConnected() {
        return isConnected;
    }

    /*Записываем заданный объект в поток*/
    public void sendObject(Object outObject) {
        try {
            oeos.writeObject(outObject);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*Читаем объект из потока и возвращаем его.
     * Если ничего не считали - вернется null*/
    public Object readObject() {
        Object incomingObj = null;
        try {
            incomingObj = odis.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return incomingObj;
    }

    /*Закрываем подключения и потоки ввода вывода*/
    public void close() {
        try {
            oeos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            odis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        isConnected = false;
    }

}
