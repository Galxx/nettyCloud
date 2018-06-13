package messages;
import java.util.List;

public class FileListMsg extends AbstractMsg {
    private List<String> fileList;

    public FileListMsg(List<String> fileList) {
        this.fileList = fileList;
    }

    public List<String> getFileList() {
        return fileList;
    }
}
