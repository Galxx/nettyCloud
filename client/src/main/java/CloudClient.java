import common.FSWorker;
import io.netty.handler.codec.serialization.ObjectDecoderInputStream;
import io.netty.handler.codec.serialization.ObjectEncoderOutputStream;
import messages.*;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class CloudClient {

    private FSWorker fsWorker;

    private List<String> filesList;
    private String rootDir;
    String address;
    int port;

    /*
     * Класс описывает основной функционал облачного клиента: чтение и отправку сообщений
     * */

    /*
     * В конструкторе создаем Socket к серверу, открываем потоки ввода/вывода
     * Создаем объект класса FSWorker, который предназначе для работы с файловой системой.
     * */
    public CloudClient(String address, int port) {
        this.address = address;
        this.port = port;
        rootDir = "D:\\ClientStorage\\";
        fsWorker = new FSWorker(rootDir);
    }

    public void connect() {
        NetworkClient.getInstance().connect(address, port);
    }


    //Запускаем чтение входящих сообщений и их обработку в отдельном потоке
    public void startReadingThread(ControllerFX controllerFX) {
        Thread thread = new Thread(() -> {

            while (NetworkClient.getInstance().isConnected()) {
                //ожидаем поступления сообщения и считываем его в объект
                Object msg = NetworkClient.getInstance().readObject();

                if (msg != null) {
                    System.out.println("One message received" + msg.toString());

                    if (msg instanceof AbstractMsg) {
                        AbstractMsg incomingMsg = (AbstractMsg) msg;
                        //если поступило сообщение с командой - обработаем его
                        if (incomingMsg instanceof CommandMsg) {

                            CommandMsg cmdMsg = (CommandMsg) incomingMsg;

                            if (cmdMsg.getCommand() == CommandMsg.AUTH_OK) {
                                System.out.println("AUTHOK");
                                controllerFX.loginOk();
                            } else if (cmdMsg.getCommand() == CommandMsg.CREATE_DIR) {
                                System.out.println("CREATE_DIR");
                                createDirectory(cmdMsg);
                            }
                        }

                        //получаем из входящего сообщения список файлой
                        if (incomingMsg instanceof FileListMsg) {
                            filesList = ((FileListMsg) incomingMsg).getFileList();
                            controllerFX.setCloudFilesList(filesList);
                        }

                        //принимаем и сохраняем в локальное хранилище файл
                        if (incomingMsg instanceof FileTransferMsg) {
                            saveFileToLocalStorage((FileTransferMsg) incomingMsg);
                        }
                    }
                }
            }

        });

        //назначаем потом - демоном, чтобы об завершил работу сразу после закрытия
        // основного приложения
        thread.setDaemon(true);
        thread.start();
    }

    //Выполняем преобразование путей и создаем каталог
    private void createDirectory(CommandMsg cmdMsg) {

        Path rootPath = Paths.get(rootDir + "\\");
        Object inObj1 = cmdMsg.getObject()[0];

        if (inObj1 instanceof String) {
            Path tempPath1 = Paths.get((String) inObj1);
            Path newPath = Paths.get(rootPath.toString(), "\\",
                    tempPath1.subpath(1, tempPath1.getNameCount()).toString());

            fsWorker.mkDir(newPath);
        }

    }

    //Метод получает на вход сообщение и сохраняет файл в локальную папку.
    //Если файл существует, то он будет перезаписан
    private void saveFileToLocalStorage(FileTransferMsg fileMsg) {
        Path newFilePath = Paths.get(rootDir +
                fileMsg.getFileName());

        fsWorker.mkFile(newFilePath, fileMsg.getData());
    }

    //getter для списка файлов
    public List<String> getCloudFilesList() {
        return filesList;
    }

    public String getRootDir() {
        return rootDir;
    }

    public void setRootDir(String newRootDir) {
        rootDir = newRootDir;
    }

    /*
     * Получает список файлов и каталогов в локальном хранилище
     * */
    public List<String> getLocalFilesList() {
        return fsWorker.listDir(Paths.get(rootDir));
    }

    /*
    метод, реализующий отправку каталога или файла со всеми файлами в облачное хранилище
    * */
    public void uploadFileOrFolder(String itemName) {
        Path path = Paths.get(getRootDir(), itemName);
        if (Files.isDirectory(path)) {
            //отправляем директорию со всем ее содержимым в облачное хранилище
            try {
                sendFolder(path);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Ошибка при отправке директории!" + itemName);
            }
        } else {
            try {
                uploadFile(path);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Ошибка при отправке файла!" + itemName);
            }
        }
    }

    //метод, реализующий отправку каталога со всеми файлами в облачное хранилище
    private void sendFolder(Path folderPath) throws IOException {
        Files.walkFileTree(folderPath, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                System.out.println("Обнаружили директорию" + dir.toString());
                NetworkClient.getInstance()
                        .sendObject(new CommandMsg(
                                CommandMsg.CREATE_DIR,
                                dir.toString(), dir.getParent().toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println("Отправляем файл " + file);
                uploadFile(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.TERMINATE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        });
    }

    //метод, реализующий отправку файлов из локального хранилища в облачное
    private void uploadFile(Path filePath) throws IOException {
        System.out.println("Send file - " + filePath.getFileName().toString());
        NetworkClient.getInstance()
                .sendObject(new FileTransferMsg(filePath));
    }

    /*
     * Отправить файл в облачное хранилище по имени
     * */
    public void sendFileToStorage(String fileName) {
        Path filePath = Paths.get(getRootDir(), fileName);
        try {
            NetworkClient.getInstance()
                    .sendObject(new FileTransferMsg(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //метод отправляет на сервер запрос на закачку файла в локальное хранилище
    public void downloadFile(String fileName) {
        NetworkClient.getInstance()
                .sendObject(new CommandMsg(CommandMsg.DOWNLOAD_FILE, fileName));
    }

    //отправляем команду на удаление файла в облачном хранилище по имени
    public void deleteCloudFsObj(String fileName) {
        NetworkClient.getInstance()
                .sendObject(new CommandMsg(CommandMsg.DELETE, fileName));
    }

    //метод выполняем удаление файла из локального хранилища по имени
    public void deleteLocalFile(String fileName) {
        Path newFilePath = Paths.get(getRootDir(), fileName);
        System.out.println(fileName);
        fsWorker.deleteFileFromStorage(newFilePath);
    }

    /*выполняем отправку аутентификационного сообщения с заданными логином - паролем*/
    public void doAuth(String login, String pwd) {
        NetworkClient.getInstance()
                .sendObject(new AuthMsg(login, pwd));
    }

    //метод отправляет команду на получение списка файлов в облачном хранилище
    public void listCloudFiles(String itemName) {
        CommandMsg cmd;

        if (itemName != null) {
            if (itemName.equals(".."))
                cmd = new CommandMsg(CommandMsg.LIST_FILES, "..");
            else
                cmd = (new CommandMsg(CommandMsg.LIST_FILES, itemName));
        } else
            cmd = (new CommandMsg(CommandMsg.LIST_FILES));

        NetworkClient.getInstance().sendObject(cmd);
    }
}
