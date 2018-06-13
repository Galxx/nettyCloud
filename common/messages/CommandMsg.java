
package messages;
public class CommandMsg extends AbstractMsg {
    private int command;
    public static final int LIST_FILES = 31646987;
    public static final int DOWNLOAD_FILE = 15791546;
    public static final int DELETE = 79719976;
    public static final int AUTH_OK = 462277345;
    public static final int CREATE_DIR = 5773788;
    //public static final int DELETE_DIR = 562129356;

    private Object[] object;

    public CommandMsg(int command, Object ... objects) {
        this.command = command;
        this.object = objects;
    }

    public int getCommand() {
        return command;
    }

    public Object[] getObject() {
        return object;
    }
}
