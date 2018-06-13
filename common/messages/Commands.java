package messages;
public enum Commands {
    DOWNLOAD(15791546),
    LIST(31646987);

    private int command;

    Commands(int command) {
        this.command = command;
    }

    public int getCommand() {
        return command;
    }
}
