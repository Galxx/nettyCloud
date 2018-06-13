import common.FSWorker;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import messages.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/*Класс предназначен для описание обработчика действий сервера при подключении к нему клиентов.
* */

public class CloudServerHandler extends ChannelInboundHandlerAdapter {

    private String nickname;
    private boolean isLogged;
    private String currentFolderPath;
    private Logger logger;
    private FSWorker fsWorker;

    public CloudServerHandler() {
        this.logger = LoggerFactory.getLogger(CloudServerHandler.class);
        fsWorker = new FSWorker();
    }



    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        try {
            if (msg == null)
                return;

            if (msg instanceof AbstractMsg) {
                processMsg((AbstractMsg) msg, ctx);
            } else {
                System.out.println("Server received wrong object!");
                logger.debug("Server received wrong object!");
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /*Данный метод вызывается в случае возникновения исключительных ситуаций*/
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.out.println(cause.getMessage());
    }

    //обрабатываем поступившее сообшение в зависимости от класса
    private void processMsg(AbstractMsg msg, ChannelHandlerContext ctx) {

        System.out.println("username: " + nickname);

        //обрабатываем остальные сообщения только, если прошли аутентификацию в БД
        if (isLogged) {
            if (msg instanceof FileTransferMsg) {
                saveFileToStorage((FileTransferMsg) msg);
            } else if (msg instanceof CommandMsg) {
                System.out.println("Server received a command " +
                        ((CommandMsg) msg).getCommand());
                logger.debug("Server received a command", msg);
                processCommand((CommandMsg) msg, ctx);
            }
        } else {
            //вызываем проверку аутентификационных данных в БД
            if (msg instanceof AuthMsg) {
                System.out.println("Nickname in CloudServerHandler" +
                        ((AuthMsg) msg).getNickname());
                checkAuth((AuthMsg) msg, ctx);
            }
        }
    }

    //обрабатываем поступившую команду и отправляем ответ на нее клиенту
    private void processCommand(CommandMsg msg, ChannelHandlerContext ctx) {

        switch (msg.getCommand()) {
            case CommandMsg.LIST_FILES:
                sendFileList(msg, ctx);
                break;
            case CommandMsg.DOWNLOAD_FILE:
                sendFile(msg, ctx);
                break;
            case CommandMsg.DELETE:
                deleteFileOrFolder(msg);
                break;
            case CommandMsg.CREATE_DIR:
                createDirectory(msg);
                break;
        }
    }

    //switch-case методы
    private void sendFileList(CommandMsg msg, ChannelHandlerContext ctx) {
        sendData(new FileListMsg(getClientFilesList(msg.getObject()[0])), ctx);
    }

    //отправить файл клиенту
    private void sendFile(CommandMsg msg, ChannelHandlerContext ctx) {
        try {
            Path filePath = Paths.get(currentFolderPath + "\\",
                    (String) (msg.getObject()[0]));
            sendData(
                    new FileTransferMsg(filePath), ctx);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //удаляем объект файловой системы  - или папку
    private void deleteFileOrFolder(CommandMsg msg) {

        String folderName = currentFolderPath + msg.getObject()[0];
        fsWorker.delFsObject(folderName);

    }

    //метод, создающий директории в облачном хранилище по команде CREATE_DIR
    private void createDirectory(CommandMsg msg) {

        logger.debug("Попытка создать директорию.");

        Path rootPath = Paths.get(currentFolderPath + "\\");
        Object inObj1 = msg.getObject()[0];

        if (inObj1 instanceof String) {
            Path tempPath1 = Paths.get((String) inObj1);
            Path newPath = Paths.get(rootPath.toString(), "\\",
                    tempPath1.subpath(1, tempPath1.getNameCount()).toString());

            fsWorker.mkDir(newPath);
        }
    }


    //получаем файл в виде объекта, записываем его в хранилище, в папку пользователя
    private void saveFileToStorage(FileTransferMsg msg) {

        //получаем путь файла из локального хранилища
        Path filePath = Paths.get(msg.getPath());

        //отбрасываем корневой каталог из локального хранилища
        String relPath = filePath.subpath(1, filePath.getNameCount()).toString();

        //складываем пути
        Path newFilePath = Paths.get(currentFolderPath + "\\" + relPath);

        //создаем файл в облачном хранилище
        fsWorker.mkFile(newFilePath, msg.getData());

    }

    //получаем список фалов в папке клиента, преобразуем в List и возвращаем
    private List<String> getClientFilesList(Object folderName) {
        List<String> fileList;
        Path listFolderPath = Paths.get(currentFolderPath);

        logger.debug("current folderName = " + folderName);
        logger.debug("current currentFolderPath = " + currentFolderPath);

        //перейти в каталог с именем folderName
        if (folderName != null) {
            //переход на уровень выше
            if (folderName.equals("..")) {
                listFolderPath = Paths.get(currentFolderPath).getParent();
                currentFolderPath = listFolderPath.toString() + "\\";
            } else {
                currentFolderPath += folderName + "\\";
                listFolderPath = Paths.get(currentFolderPath);
            }
        }

        System.out.println("current currentFolderPath = " + currentFolderPath);
        fileList = fsWorker.listDir(listFolderPath);

        return fileList;
    }

    //устанавливаем флаг isLogged и заполняем поле nickname
    private void checkAuth(AuthMsg incomingMsg, ChannelHandlerContext ctx) {
        if (incomingMsg != null) {

            //получаем значение nickname из Auth Handler
            nickname = incomingMsg.getNickname();
            if (nickname != null) {

                //установить начальное значение папки просмотра
                String rootDir = "D:\\ServerStorage\\";
                currentFolderPath = rootDir + nickname + "\\";

                System.out.println("Client Auth OK");
                isLogged = true;
                fsWorker.setRootDir(currentFolderPath);

                sendData(new CommandMsg(CommandMsg.AUTH_OK), ctx);

                logger.debug("Client Auth OK");
            } else {
                System.out.println("Client not found");
                isLogged = false;
                logger.debug("Client not found");
            }
        }
    }

    //метод для отправки данных
    private void sendData(AbstractMsg msg, ChannelHandlerContext ctx) {
        ctx.writeAndFlush(msg);
    }


}
