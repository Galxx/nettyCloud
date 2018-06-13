package common;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class FSWorker {

    /*
    Класс предназначен для работы с файловым хранилищем,
    описывает общие для клиентов и сервера методы.
    В конструкторе задаем корневой каталог.
    * */

    /*Поле предназначено для хранения корневого каталога.
     * На клиенте - это будет его папка на HDD, для сервера - каталог,
     * соответствующий nickname подключившегося клиента */
    private String rootDir;

    /*Конструкторы*/
    public FSWorker(String rootDir) {
        this.rootDir = rootDir;
    }

    public FSWorker() {

    }

    /*
     * Setter для установки значения поля - корневого каталога пользователя
     * */
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

    /*
    Методы для работы с файлами и каталогами
    * */

    /*
     * Просмотр содержимого каталога.
     * На вход метода подается путь к каталогу.
     * */
    public List<String> listDir(Path dirPath) {
        List<String> fileList = new ArrayList<>();

        try (DirectoryStream<Path> str = Files.newDirectoryStream(dirPath)) {

            str.forEach(path -> fileList.add(
                    path.getFileName().toString()));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileList;
    }

    /*
    * Создание каталога
    * */
    public void mkDir(Path newPath) {
        try {
            Files.createDirectories(newPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ошибка создания директории!");
//                logger.error("Ошибка создания директории!");
//                logger.error(e.getMessage());
        }
    }

    /*
    * Создание файла
    * */
    public void mkFile(Path newFilePath, byte[] data) {
        StandardOpenOption sOption;

        if (Files.exists(newFilePath)) {
            sOption = StandardOpenOption.TRUNCATE_EXISTING;
        } else {
            sOption = StandardOpenOption.CREATE;
        }

        try {
            Files.write(newFilePath, data, sOption);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
    Удаляем файл или каталог с файлами
    * */
    public void delFsObject(String fsObjectName) {

        Path newPath = Paths.get(fsObjectName);
        Path pathToDelete = Paths.get(rootDir, "\\",
                newPath.subpath(2, newPath.getNameCount()).toString());

        if (Files.isDirectory(pathToDelete)) {
            deleteDirectory(pathToDelete);

        } else if (Files.isRegularFile(pathToDelete)) {
            deleteFileFromStorage(pathToDelete);

        }
    }

    //удаление файла из хранилища по пути к файлу
    public void deleteFileFromStorage(Path pathToDelete) {
        try {
            Files.delete(pathToDelete);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //удаление каталога с файлами
    private void deleteDirectory(Path pathToDelete) {
//        try (Stream<Path> str = Files.list(pathToDelete)) {
//            str.sorted(Comparator.reverseOrder())
//                    .map(Path::toFile)
//                    .forEach(File::delete);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        //удалаяем сам пустой каталог
//            //Files.delete(pathToDelete);
//        }

        try {
            Files.walkFileTree(pathToDelete, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    System.out.println("delete file: " + file.toString());
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    System.out.println("delete dir: " + dir.toString());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }


//        }
//
//        new Thread(() -> {
//            try () {
//                FileUtils.deleteDirectory(pathToDelete.toFile());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }).start();

//        fullFolderPath = rootDir + nickname;

//        try {
//            Files.walk(pathToDirToDelete, FileVisitOption.FOLLOW_LINKS)
//                    .sorted(Comparator.reverseOrder())
//                    .forEach(path -> {
//                        try {
//                            Files.delete(path);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    });
//            //Files.delete(pathToDirToDelete);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }
}
